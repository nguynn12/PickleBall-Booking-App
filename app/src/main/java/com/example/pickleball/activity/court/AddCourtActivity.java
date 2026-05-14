package com.example.pickleball.activity.court;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.pickleball.R;
import com.example.pickleball.model.Court;
import com.example.pickleball.utils.ImageHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddCourtActivity extends AppCompatActivity {

    public static final String EXTRA_COURT = "EXTRA_COURT";

    private TextInputEditText edtName, edtAddress, edtPhone, edtPrice,
            edtOpenTime, edtCloseTime, edtDescription;
    private Chip chipPickleball, chipIndoor, chipOutdoor;
    private MaterialButton btnSave, btnPickImage;
    private ImageView imgPreview;
    private TextView tvTitle;

    private Court editCourt;
    private String uploadedImageUrl = ""; // URL sau khi upload xong
    private ProgressDialog progressDialog;

    // Image picker
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_court);

        editCourt = (Court) getIntent().getSerializableExtra(EXTRA_COURT);

        initViews();
        setupImagePicker();
        if (editCourt != null) prefillForEdit();

        ((ImageView) findViewById(R.id.btnBack)).setOnClickListener(v -> finish());
        btnPickImage.setOnClickListener(v -> checkPermissionAndPickImage());
        btnSave.setOnClickListener(v -> saveCourt());
    }

    // ─── INIT ────────────────────────────────────────────────────────────────

    private void initViews() {
        tvTitle        = findViewById(R.id.tvTitle);
        imgPreview     = findViewById(R.id.imgCourtPreview);
        edtName        = findViewById(R.id.edtCourtName);
        edtAddress     = findViewById(R.id.edtAddress);
        edtPhone       = findViewById(R.id.edtPhone);
        edtPrice       = findViewById(R.id.edtPrice);
        edtOpenTime    = findViewById(R.id.edtOpenTime);
        edtCloseTime   = findViewById(R.id.edtCloseTime);
        edtDescription = findViewById(R.id.edtDescription);
        chipPickleball = findViewById(R.id.chipPickleball);
        chipIndoor     = findViewById(R.id.chipIndoor);
        chipOutdoor    = findViewById(R.id.chipOutdoor);
        btnSave        = findViewById(R.id.btnSave);
        btnPickImage   = findViewById(R.id.btnPickImage);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        // Chỉ 1 chip được chọn
        chipPickleball.setOnClickListener(v -> { chipPickleball.setChecked(true); chipIndoor.setChecked(false); chipOutdoor.setChecked(false); });
        chipIndoor.setOnClickListener(v    -> { chipPickleball.setChecked(false); chipIndoor.setChecked(true);  chipOutdoor.setChecked(false); });
        chipOutdoor.setOnClickListener(v   -> { chipPickleball.setChecked(false); chipIndoor.setChecked(false); chipOutdoor.setChecked(true);  });
    }

    // ─── IMAGE PICKER ────────────────────────────────────────────────────────

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) handleImageSelected(uri);
                    }
                });

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) openGallery();
                    else Toast.makeText(this, "Cần cấp quyền để chọn ảnh!", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkPermissionAndPickImage() {
        String perm = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            permissionLauncher.launch(perm);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void handleImageSelected(Uri uri) {
        if (!ImageHelper.isValidImageUri(this, uri)) {
            Toast.makeText(this, "Ảnh không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }
        // Preview ngay
        Glide.with(this).load(uri).centerCrop().into(imgPreview);
        imgPreview.setPadding(0, 0, 0, 0);

        // Compress + upload
        byte[] data = ImageHelper.compressImage(this, uri);
        if (data == null) {
            Toast.makeText(this, "Lỗi xử lý ảnh!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Đang tải ảnh lên... 0%");
        progressDialog.show();

        String fileName = "court_images/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance().getReference(fileName);
        ref.putBytes(data)
                .addOnProgressListener(snap -> {
                    int pct = (int) (100.0 * snap.getBytesTransferred() / snap.getTotalByteCount());
                    progressDialog.setMessage("Đang tải ảnh lên... " + pct + "%");
                })
                .addOnSuccessListener(snap -> ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    progressDialog.dismiss();
                    uploadedImageUrl = downloadUri.toString();
                    Toast.makeText(this, "Tải ảnh thành công!", Toast.LENGTH_SHORT).show();
                }))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ─── PREFILL (edit mode) ─────────────────────────────────────────────────

    private void prefillForEdit() {
        tvTitle.setText("Chỉnh sửa sân");
        btnSave.setText("Cập nhật");

        edtName.setText(editCourt.getCourtName());
        edtAddress.setText(editCourt.getAddress());
        edtPhone.setText(editCourt.getPhone());
        if (editCourt.getPricePerHour() > 0)
            edtPrice.setText(String.valueOf((long) editCourt.getPricePerHour()));
        edtOpenTime.setText(editCourt.getOpenTime() != null ? editCourt.getOpenTime() : "06:00");
        edtCloseTime.setText(editCourt.getCloseTime() != null ? editCourt.getCloseTime() : "22:00");
        edtDescription.setText(editCourt.getDescription());

        String type = editCourt.getType() != null ? editCourt.getType() : "";
        chipPickleball.setChecked(type.contains("Pickleball") || type.isEmpty());
        chipIndoor.setChecked(type.contains("Trong nhà"));
        chipOutdoor.setChecked(type.contains("Ngoài trời"));

        // Load ảnh hiện tại
        if (editCourt.getImageUrl() != null && !editCourt.getImageUrl().isEmpty()) {
            uploadedImageUrl = editCourt.getImageUrl();
            Glide.with(this).load(uploadedImageUrl).centerCrop().into(imgPreview);
            imgPreview.setPadding(0, 0, 0, 0);
        }
    }

    // ─── SAVE ────────────────────────────────────────────────────────────────

    private void saveCourt() {
        String name  = getText(edtName);
        String addr  = getText(edtAddress);
        String price = getText(edtPrice);

        if (name.isEmpty() || addr.isEmpty() || price.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin bắt buộc (*)", Toast.LENGTH_SHORT).show();
            return;
        }

        double priceVal;
        try { priceVal = Double.parseDouble(price); }
        catch (NumberFormatException e) {
            Toast.makeText(this, "Giá tiền không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = chipIndoor.isChecked() ? "Trong nhà"
                : chipOutdoor.isChecked() ? "Ngoài trời" : "Pickleball";

        btnSave.setEnabled(false);
        btnSave.setText("Đang lưu...");

        if (editCourt == null) {
            // Thêm mới
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            Court court = new Court(null, name, addr, type, priceVal, uploadedImageUrl);
            court.setOwnerId(uid);
            court.setPhone(getText(edtPhone));
            court.setOpenTime(getText(edtOpenTime).isEmpty() ? "06:00" : getText(edtOpenTime));
            court.setCloseTime(getText(edtCloseTime).isEmpty() ? "22:00" : getText(edtCloseTime));
            court.setDescription(getText(edtDescription));
            court.setStatus("active");
            court.setCreatedAt(System.currentTimeMillis());

            FirebaseFirestore.getInstance().collection("Courts")
                    .add(court)
                    .addOnSuccessListener(ref -> {
                        ref.update("courtId", ref.getId());
                        Toast.makeText(this, "Thêm sân thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> resetButton("Lưu thông tin", e.getMessage()));
        } else {
            // Cập nhật
            Map<String, Object> updates = new HashMap<>();
            updates.put("courtName",    name);
            updates.put("address",      addr);
            updates.put("type",         type);
            updates.put("pricePerHour", priceVal);
            updates.put("phone",        getText(edtPhone));
            updates.put("openTime",     getText(edtOpenTime).isEmpty() ? "06:00" : getText(edtOpenTime));
            updates.put("closeTime",    getText(edtCloseTime).isEmpty() ? "22:00" : getText(edtCloseTime));
            updates.put("description",  getText(edtDescription));
            if (!uploadedImageUrl.isEmpty()) updates.put("imageUrl", uploadedImageUrl);

            FirebaseFirestore.getInstance().collection("Courts")
                    .document(editCourt.getCourtId())
                    .update(updates)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, "Cập nhật sân thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> resetButton("Cập nhật", e.getMessage()));
        }
    }

    private void resetButton(String text, String errMsg) {
        btnSave.setEnabled(true);
        btnSave.setText(text);
        Toast.makeText(this, "Lỗi: " + errMsg, Toast.LENGTH_SHORT).show();
    }

    private String getText(TextInputEditText edt) {
        return edt.getText() != null ? edt.getText().toString().trim() : "";
    }
}
