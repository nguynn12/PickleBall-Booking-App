package com.example.pickleball.activity.profile;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.example.pickleball.R;
import com.example.pickleball.activity.auth.LoginActivity;
import com.example.pickleball.utils.Constants;
import com.example.pickleball.utils.ImageHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageView imgAvatar;
    private TextView tvChangeAvatar;
    private TextInputEditText edtName, edtPhone;
    private TextView tvEmail, tvRole, tvUserName;
    private MaterialButton btnSaveProfile;
    private LinearLayout btnChangePassword, btnSettings, btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUid;
    private String currentAvatarUrl;

    // Image picker launcher
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        currentUid = user.getUid();

        // Initialize views
        imgAvatar       = findViewById(R.id.imgAvatar);
        tvChangeAvatar  = findViewById(R.id.tvChangeAvatar);
        edtName         = findViewById(R.id.edtName);
        edtPhone        = findViewById(R.id.edtPhone);
        tvEmail         = findViewById(R.id.tvEmail);
        tvRole          = findViewById(R.id.tvRole);
        tvUserName      = findViewById(R.id.tvUserName);
        btnSaveProfile  = findViewById(R.id.btnSaveProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnSettings     = findViewById(R.id.btnSettings);
        btnLogout       = findViewById(R.id.btnLogout);

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tải lên...");
        progressDialog.setCancelable(false);

        // Setup image picker launcher
        setupImagePicker();

        loadUserProfile();

        // Click listeners
        imgAvatar.setOnClickListener(v -> checkPermissionAndPickImage());
        tvChangeAvatar.setOnClickListener(v -> checkPermissionAndPickImage());
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
        btnLogout.setOnClickListener(v -> logout());

        // Nút thông báo
        android.view.View btnNotif = findViewById(R.id.btnGoNotifications);
        if (btnNotif != null) {
            btnNotif.setOnClickListener(v ->
                    startActivity(new Intent(this, com.example.pickleball.activity.NotificationsActivity.class)));
        }
    }

    // ─── IMAGE PICKER SETUP ──────────────────────────────────────────────────
    private void setupImagePicker() {
        // Image picker result
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        handleImageSelected(imageUri);
                    }
                }
            }
        );

        // Permission result
        permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    showImagePickerDialog();
                } else {
                    Toast.makeText(this, "Cần cấp quyền để chọn ảnh!", Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    private void checkPermissionAndPickImage() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                showImagePickerDialog();
            } else {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // Below Android 13
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                showImagePickerDialog();
            } else {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void showImagePickerDialog() {
        String[] options = {"Chọn từ thư viện", "Hủy"};

        new AlertDialog.Builder(this)
            .setTitle("Chọn ảnh đại diện")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // Gallery
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    imagePickerLauncher.launch(intent);
                }
            })
            .show();
    }

    private void handleImageSelected(Uri imageUri) {
        // Validate image
        if (!ImageHelper.isValidImageUri(this, imageUri)) {
            Toast.makeText(this, "Ảnh không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show preview
        Glide.with(this)
            .load(imageUri)
            .circleCrop()
            .into(imgAvatar);

        // Compress and upload
        uploadAvatar(imageUri);
    }

    private void uploadAvatar(Uri imageUri) {
        // Compress image
        byte[] imageData = ImageHelper.compressImage(this, imageUri);
        if (imageData == null) {
            Toast.makeText(this, "Lỗi xử lý ảnh!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        progressDialog.show();
        progressDialog.setMessage("Đang tải lên... 0%");

        // Upload to Firebase Storage
        ImageHelper.uploadAvatar(currentUid, imageData, new ImageHelper.UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                progressDialog.dismiss();
                currentAvatarUrl = downloadUrl;

                // Update Firestore
                updateAvatarUrlInFirestore(downloadUrl);
            }

            @Override
            public void onFailure(Exception e) {
                progressDialog.dismiss();
                Toast.makeText(ProfileActivity.this,
                    "Lỗi tải lên: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress(int progress) {
                progressDialog.setMessage("Đang tải lên... " + progress + "%");
            }
        });
    }

    private void updateAvatarUrlInFirestore(String avatarUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(Constants.FIELD_AVATAR_URL, avatarUrl);

        db.collection(Constants.COLLECTION_USERS)
            .document(currentUid)
            .update(updates)
            .addOnSuccessListener(v -> {
                Toast.makeText(this, Constants.SUCCESS_UPLOAD, Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    // ─── LOAD PROFILE ────────────────────────────────────────────────────────
    private void loadUserProfile() {
        db.collection(Constants.COLLECTION_USERS).document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString(Constants.FIELD_FULL_NAME);
                        edtName.setText(name);
                        edtPhone.setText(doc.getString(Constants.FIELD_PHONE));
                        tvEmail.setText(doc.getString(Constants.FIELD_EMAIL));
                        tvUserName.setText(name != null ? name : "Người dùng");
                        tvRole.setText(getRoleDisplay(doc.getString(Constants.FIELD_ROLE)));

                        // Load avatar with Glide
                        currentAvatarUrl = doc.getString(Constants.FIELD_AVATAR_URL);
                        if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
                            Glide.with(this)
                                .load(currentAvatarUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_default_avatar)
                                .error(R.drawable.ic_default_avatar)
                                .into(imgAvatar);
                        }
                    }
                });
    }

    private String getRoleDisplay(String role) {
        if ("admin".equals(role)) return "Quản trị viên";
        if ("owner".equals(role)) return "Chủ sân";
        return "Khách hàng";
    }

    // ─── SAVE PROFILE ────────────────────────────────────────────────────────
    private void saveProfile() {
        String name  = edtName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, Constants.ERROR_EMPTY_FIELDS, Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(Constants.FIELD_FULL_NAME, name);
        updates.put(Constants.FIELD_PHONE, phone);

        db.collection(Constants.COLLECTION_USERS).document(currentUid).update(updates)
            .addOnSuccessListener(v -> {
                tvUserName.setText(name);
                Toast.makeText(this, Constants.SUCCESS_UPDATE, Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e ->
                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ─── CHANGE PASSWORD ─────────────────────────────────────────────────────
    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Đổi mật khẩu");

                android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(60, 20, 60, 20);

        android.widget.EditText edtNewPass = new android.widget.EditText(this);
        edtNewPass.setHint("Mật khẩu mới (ít nhất 6 ký tự)");
                edtNewPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(edtNewPass);
        builder.setView(layout);

        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
                String newPass = edtNewPass.getText().toString().trim();
        if (newPass.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải từ 6 ký tự!", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.updatePassword(newPass)
                    .addOnSuccessListener(v ->
                            Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Cần đăng nhập lại trước khi đổi mật khẩu!", Toast.LENGTH_SHORT).show());
        }
    });
        builder.setNegativeButton("Hủy", null);
                builder.show();
}

    // ─── LOGOUT ──────────────────────────────────────────────────────────────
    private void logout() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.logout_confirm_title)
            .setMessage(R.string.logout_confirm_message)
            .setPositiveButton(R.string.yes, (dialog, which) -> {
                mAuth.signOut();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            })
            .setNegativeButton(R.string.no, null)
            .show();
    }
}
