package com.example.pickleball.activity;

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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText edtEmail, edtPassword;
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
        btnLogin       = findViewById(R.id.btnLogin);
        btnGoogleSignIn= findViewById(R.id.btnGoogleSignIn);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Cau hinh Google Sign-In
        String webClientId = getString(R.string.default_web_client_id);
        GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail();
        if (webClientId != null && !webClientId.trim().isEmpty() && !"CHANGE_ME".equals(webClientId)) {
            gsoBuilder.requestIdToken(webClientId);
        }
        GoogleSignInOptions gso = gsoBuilder.build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String pass  = edtPassword.getText().toString().trim();
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }
            loginWithEmail(email, pass);
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

    // ─── DANG NHAP BANG EMAIL ────────────────────────────────────────────────
    private void loginWithEmail(String email, String pass) {
        btnLogin.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        fetchRoleAndNavigate(mAuth.getCurrentUser().getUid());
                    } else {
                        btnLogin.setEnabled(true);
                        Toast.makeText(this, "Sai email hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
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
        FirebaseFirestore.getInstance().collection("Users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        // Lan dau dang nhap Google → tao User moi voi role "user"
                        User newUser = new User(
                                uid,
                                firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Người dùng",
                                firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "",
                                "",      // chua co phone
                                "user",  // mac dinh la khach hang
                                "beginner"
                        );
                        FirebaseFirestore.getInstance()
                                .collection("Users").document(uid).set(newUser)
                                        .addOnSuccessListener(v -> {
                                            Toast.makeText(this, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show();
                                                    fetchRoleAndNavigate(uid);
                                        });
                    } else {
                        // Da co tai khoan → chuyen thang vao trang chu
                        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                                fetchRoleAndNavigate(uid);
                    }
                });
    }

    // ─── LAY ROLE VA CHUYEN TRANG ────────────────────────────────────────────
    private void fetchRoleAndNavigate(String uid) {
        FirebaseFirestore.getInstance().collection("Users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                            SplashActivity.navigateByRole(this, role);
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    btnGoogleSignIn.setEnabled(true);
                    Toast.makeText(this, "Lỗi lấy thông tin!", Toast.LENGTH_SHORT).show();
                });
    }
}
