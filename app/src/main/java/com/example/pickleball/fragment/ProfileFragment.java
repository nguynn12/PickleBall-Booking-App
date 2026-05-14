package com.example.pickleball.fragment;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.pickleball.R;
import com.example.pickleball.activity.auth.LoginActivity;
import com.example.pickleball.activity.profile.SettingsActivity;
import com.example.pickleball.utils.Constants;
import com.example.pickleball.utils.ImageHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private ImageView imgAvatar;
    private TextView tvChangeAvatar, tvEmail, tvRole, tvUserName;
    private TextInputEditText edtName, edtPhone;
    private MaterialButton btnSaveProfile;
    private LinearLayout btnChangePassword, btnSettings, btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUid;
    private String currentAvatarUrl;
    private ProgressDialog progressDialog;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            // Chưa đăng nhập → hiển thị màn hình mời đăng nhập
            showGuestView(view);
            return;
        }
        currentUid = user.getUid();

        imgAvatar        = view.findViewById(R.id.imgAvatar);
        tvChangeAvatar   = view.findViewById(R.id.tvChangeAvatar);
        edtName          = view.findViewById(R.id.edtName);
        edtPhone         = view.findViewById(R.id.edtPhone);
        tvEmail          = view.findViewById(R.id.tvEmail);
        tvRole           = view.findViewById(R.id.tvRole);
        tvUserName       = view.findViewById(R.id.tvUserName);
        btnSaveProfile   = view.findViewById(R.id.btnSaveProfile);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnSettings      = view.findViewById(R.id.btnSettings);
        btnLogout        = view.findViewById(R.id.btnLogout);

        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Đang tải lên...");
        progressDialog.setCancelable(false);

        setupImagePicker();
        loadUserProfile();

        imgAvatar.setOnClickListener(v -> checkPermissionAndPickImage());
        tvChangeAvatar.setOnClickListener(v -> checkPermissionAndPickImage());
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));
        btnLogout.setOnClickListener(v -> logout());

        // Nút thông báo
        View btnNotif = view.findViewById(R.id.btnGoNotifications);
        if (btnNotif != null) {
            btnNotif.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(),
                            com.example.pickleball.activity.NotificationsActivity.class)));
        }
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == requireActivity().RESULT_OK
                            && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) handleImageSelected(uri);
                    }
                });

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) showImagePickerDialog();
                    else Toast.makeText(requireContext(), "Cần cấp quyền để chọn ảnh!", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkPermissionAndPickImage() {
        String perm = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(requireContext(), perm) == PackageManager.PERMISSION_GRANTED) {
            showImagePickerDialog();
        } else {
            permissionLauncher.launch(perm);
        }
    }

    private void showImagePickerDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn ảnh đại diện")
                .setItems(new String[]{"Chọn từ thư viện", "Hủy"}, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        imagePickerLauncher.launch(intent);
                    }
                }).show();
    }

    private void handleImageSelected(Uri uri) {
        if (!ImageHelper.isValidImageUri(requireContext(), uri)) {
            Toast.makeText(requireContext(), "Ảnh không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }
        Glide.with(this).load(uri).circleCrop().into(imgAvatar);
        byte[] data = ImageHelper.compressImage(requireContext(), uri);
        if (data == null) {
            Toast.makeText(requireContext(), "Lỗi xử lý ảnh!", Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog.show();
        ImageHelper.uploadAvatar(currentUid, data, new ImageHelper.UploadCallback() {
            @Override public void onSuccess(String url) {
                progressDialog.dismiss();
                currentAvatarUrl = url;
                Map<String, Object> upd = new HashMap<>();
                upd.put(Constants.FIELD_AVATAR_URL, url);
                db.collection(Constants.COLLECTION_USERS).document(currentUid).update(upd)
                        .addOnSuccessListener(v -> Toast.makeText(requireContext(), "Cập nhật ảnh thành công!", Toast.LENGTH_SHORT).show());
            }
            @Override public void onFailure(Exception e) {
                progressDialog.dismiss();
                Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            @Override public void onProgress(int progress) {
                progressDialog.setMessage("Đang tải lên... " + progress + "%");
            }
        });
    }

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
                        currentAvatarUrl = doc.getString(Constants.FIELD_AVATAR_URL);
                        if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
                            Glide.with(this).load(currentAvatarUrl).circleCrop()
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

    private void saveProfile() {
        String name  = edtName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(requireContext(), Constants.ERROR_EMPTY_FIELDS, Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> upd = new HashMap<>();
        upd.put(Constants.FIELD_FULL_NAME, name);
        upd.put(Constants.FIELD_PHONE, phone);
        db.collection(Constants.COLLECTION_USERS).document(currentUid).update(upd)
                .addOnSuccessListener(v -> {
                    tvUserName.setText(name);
                    Toast.makeText(requireContext(), Constants.SUCCESS_UPDATE, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showChangePasswordDialog() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(60, 20, 60, 20);
        android.widget.EditText edtNewPass = new android.widget.EditText(requireContext());
        edtNewPass.setHint("Mật khẩu mới (ít nhất 6 ký tự)");
        edtNewPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(edtNewPass);

        new AlertDialog.Builder(requireContext())
                .setTitle("Đổi mật khẩu")
                .setView(layout)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    String newPass = edtNewPass.getText().toString().trim();
                    if (newPass.length() < 6) {
                        Toast.makeText(requireContext(), "Mật khẩu phải từ 6 ký tự!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        user.updatePassword(newPass)
                                .addOnSuccessListener(v -> Toast.makeText(requireContext(), "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Cần đăng nhập lại trước!", Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void logout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Có", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Không", null)
                .show();
    }

    /** Hiển thị màn hình mời đăng nhập cho khách chưa đăng nhập */
    private void showGuestView(View rootView) {
        // Ẩn tất cả view profile
        rootView.setVisibility(android.view.View.GONE);

        // Tạo view mời đăng nhập
        android.widget.FrameLayout container = (android.widget.FrameLayout)
                ((android.view.ViewGroup) rootView.getParent());

        android.widget.LinearLayout guestLayout = new android.widget.LinearLayout(requireContext());
        guestLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        guestLayout.setGravity(android.view.Gravity.CENTER);
        guestLayout.setBackgroundColor(requireContext().getColor(R.color.bg_primary));
        guestLayout.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        guestLayout.setPadding(48, 0, 48, 0);

        android.widget.TextView tvIcon = new android.widget.TextView(requireContext());
        tvIcon.setText("👤");
        tvIcon.setTextSize(64f);
        tvIcon.setGravity(android.view.Gravity.CENTER);
        tvIcon.setPadding(0, 0, 0, 24);
        guestLayout.addView(tvIcon);

        android.widget.TextView tvTitle = new android.widget.TextView(requireContext());
        tvTitle.setText("Đăng nhập để sử dụng đầy đủ tính năng");
        tvTitle.setTextSize(18f);
        tvTitle.setTextColor(requireContext().getColor(R.color.text_primary));
        tvTitle.setGravity(android.view.Gravity.CENTER);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 12);
        guestLayout.addView(tvTitle);

        android.widget.TextView tvDesc = new android.widget.TextView(requireContext());
        tvDesc.setText("✅ Lưu lịch sử đặt sân\n✅ Nhận thông báo xác nhận\n✅ Quản lý hồ sơ cá nhân\n✅ Đánh giá sân sau khi chơi");
        tvDesc.setTextSize(14f);
        tvDesc.setTextColor(requireContext().getColor(R.color.text_secondary));
        tvDesc.setGravity(android.view.Gravity.START);
        tvDesc.setLineSpacing(8f, 1f);
        tvDesc.setPadding(0, 0, 0, 32);
        guestLayout.addView(tvDesc);

        android.widget.Button btnLogin = new android.widget.Button(requireContext());
        btnLogin.setText("Đăng nhập");
        btnLogin.setAllCaps(false);
        btnLogin.setTextSize(16f);
        btnLogin.setTextColor(requireContext().getColor(android.R.color.white));
        btnLogin.setBackgroundResource(R.drawable.bg_button_primary);
        android.widget.LinearLayout.LayoutParams btnLp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        btnLp.bottomMargin = dp(12);
        btnLogin.setLayoutParams(btnLp);
        btnLogin.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LoginActivity.class)));
        guestLayout.addView(btnLogin);

        android.widget.Button btnRegister = new android.widget.Button(requireContext());
        btnRegister.setText("Tạo tài khoản mới");
        btnRegister.setAllCaps(false);
        btnRegister.setTextSize(16f);
        btnRegister.setTextColor(requireContext().getColor(R.color.green_primary));
        btnRegister.setBackgroundResource(R.drawable.selector_button_outline);
        btnRegister.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(56)));
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(requireContext(),
                        com.example.pickleball.activity.auth.RegisterActivity.class)));
        guestLayout.addView(btnRegister);

        container.addView(guestLayout);
    }

    private int dp(int value) {
        return (int) (value * requireContext().getResources().getDisplayMetrics().density);
    }
}
