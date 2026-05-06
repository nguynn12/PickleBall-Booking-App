package com.example.pickleball.activity.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pickleball.R;
import com.example.pickleball.model.User;
import com.example.pickleball.utils.Constants;
import com.example.pickleball.utils.ValidationUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText edtEmail, edtPassword;
    private TextInputLayout tilEmail, tilPassword;
    private MaterialButton btnLogin, btnGoogleSignIn;
    private TextView tvGoToRegister, tvForgotPassword;

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;

    // Launcher cho Google Sign-In
    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    this::handleGoogleSignInResult
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        edtEmail       = findViewById(R.id.edtEmailLogin);
        edtPassword    = findViewById(R.id.edtPasswordLogin);
        tilEmail       = findViewById(R.id.tilEmailLogin);
        tilPassword    = findViewById(R.id.tilPasswordLogin);
        btnLogin       = findViewById(R.id.btnLogin);
        btnGoogleSignIn= findViewById(R.id.btnGoogleSignIn);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Cau hinh Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // lay tu google-services.json
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnLogin.setOnClickListener(v -> {
            if (validateLoginForm()) {
                String email = edtEmail.getText().toString().trim();
                String pass  = edtPassword.getText().toString();
                loginWithEmail(email, pass);
            }
        });

        btnGoogleSignIn.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        tvGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    // ─── VALIDATION ──────────────────────────────────────────────────────────
    private boolean validateLoginForm() {
        clearErrors();

        String email = edtEmail.getText().toString().trim();
        String pass  = edtPassword.getText().toString();

        boolean isValid = true;

        // Validate email
        String emailError = ValidationUtils.getEmailError(email);
        if (emailError != null) {
            tilEmail.setError(emailError);
            isValid = false;
        }

        // Validate password (basic check)
        if (!ValidationUtils.isNotEmpty(pass)) {
            tilPassword.setError("Mật khẩu không được để trống!");
            isValid = false;
        }

        return isValid;
    }

    private void clearErrors() {
        tilEmail.setError(null);
        tilPassword.setError(null);
    }

    // ─── DANG NHAP BANG EMAIL ────────────────────────────────────────────────
    private void loginWithEmail(String email, String pass) {
        btnLogin.setEnabled(false);
        btnLogin.setText(R.string.loading);

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        fetchRoleAndNavigate(mAuth.getCurrentUser().getUid());
                    } else {
                        btnLogin.setEnabled(true);
                        btnLogin.setText(R.string.btn_login);
                        Toast.makeText(this, Constants.ERROR_WRONG_CREDENTIALS, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ─── XU LY KET QUA GOOGLE SIGN-IN ───────────────────────────────────────
    private void handleGoogleSignInResult(ActivityResult result) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            Toast.makeText(this, "Google Sign-In thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        btnGoogleSignIn.setEnabled(false);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveGoogleUserIfNew(user);
                        }
                    } else {
                        btnGoogleSignIn.setEnabled(true);
                        Toast.makeText(this, "Xác thực Firebase thất bại!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** Neu la nguoi dung moi (dang nhap Google lan dau) → luu vao Firestore */
    private void saveGoogleUserIfNew(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();
        FirebaseFirestore.getInstance().collection(Constants.COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        // Lan dau dang nhap Google → tao User moi voi role "user"
                        User newUser = new User(
                                uid,
                                firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Người dùng",
                                firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "",
                                "",      // chua co phone
                                Constants.ROLE_CUSTOMER,  // mac dinh la khach hang
                                Constants.SKILL_BEGINNER
                        );
                        FirebaseFirestore.getInstance()
                                .collection(Constants.COLLECTION_USERS).document(uid).set(newUser)
                                        .addOnSuccessListener(v -> {
                                            Toast.makeText(this, Constants.SUCCESS_LOGIN, Toast.LENGTH_SHORT).show();
                                            fetchRoleAndNavigate(uid);
                                        });
                    } else {
                        // Da co tai khoan → chuyen thang vao trang chu
                        Toast.makeText(this, Constants.SUCCESS_LOGIN, Toast.LENGTH_SHORT).show();
                        fetchRoleAndNavigate(uid);
                    }
                });
    }

    // ─── LAY ROLE VA CHUYEN TRANG ────────────────────────────────────────────
    private void fetchRoleAndNavigate(String uid) {
        FirebaseFirestore.getInstance().collection(Constants.COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                            SplashActivity.navigateByRole(this, role);
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText(R.string.btn_login);
                    btnGoogleSignIn.setEnabled(true);
                    Toast.makeText(this, "Lỗi lấy thông tin!", Toast.LENGTH_SHORT).show();
                });
    }
}
