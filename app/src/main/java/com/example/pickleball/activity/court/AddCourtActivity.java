package com.example.pickleball.activity.court;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.bumptech.glide.Glide;
import com.example.pickleball.R;
import com.example.pickleball.model.Court;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AddCourtActivity extends AppCompatActivity {

    public static final String EXTRA_COURT = "EXTRA_COURT";

    private TextInputEditText edtName, edtAddress, edtPhone, edtPrice,
            edtOpenTime, edtCloseTime, edtDescription;
    private Chip chipPickleball, chipIndoor, chipOutdoor;
    private MaterialButton btnSave, btnPickImage;
    private ImageView imgPreview;
    private TextView tvTitle;

    private Court editCourt;

    // Biến lưu trữ chuỗi ảnh thay cho URL Firebase Storage
    private String base64Image = "";

    // Image picker mới gọn nhẹ hơn
    private ActivityResultLauncher<String> pickImageLauncher;

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

        // Gọi launcher lấy ảnh trực tiếp
        btnPickImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
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

        // Chỉ 1 chip được chọn
        chipPickleball.setOnClickListener(v -> { chipPickleball.setChecked(true); chipIndoor.setChecked(false); chipOutdoor.setChecked(false); });
        chipIndoor.setOnClickListener(v    -> { chipPickleball.setChecked(false); chipIndoor.setChecked(true);  chipOutdoor.setChecked(false); });
        chipOutdoor.setOnClickListener(v   -> { chipPickleball.setChecked(false); chipIndoor.setChecked(false); chipOutdoor.setChecked(true);  });
    }

    // ─── IMAGE PICKER & BASE64 ───────────────────────────────────────────────

    private void setupImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        // 1. GỠ BỎ TINT (LỚP PHỦ MÀU XANH) VÀ BACKGROUND MẶC ĐỊNH
                        imgPreview.setColorFilter(null);
                        imgPreview.setImageTintList(null);
                        imgPreview.setBackgroundResource(0);
                        imgPreview.setPadding(0, 0, 0, 0);

                        // 2. Hiển thị ảnh lên giao diện
                        Glide.with(this).load(uri).centerCrop().into(imgPreview);

                        // 3. Chuyển ảnh thành chuỗi mã hóa
                        base64Image = encodeImageToBase64(uri);
                        Toast.makeText(this, "Đã đính kèm ảnh thành công!", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private String encodeImageToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Nén ảnh xuống còn tối đa 600px để không làm quá tải Firestore
            int maxWidth = 600;
            int maxHeight = (int) (maxWidth * ((float) bitmap.getHeight() / bitmap.getWidth()));
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, maxHeight, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            byte[] imageBytes = baos.toByteArray();

            // Thêm tiền tố để thư viện Glide có thể dễ dàng đọc được
            return "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
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
            base64Image = editCourt.getImageUrl();

            // Xử lý hiển thị nếu là ảnh Base64
            if (base64Image.startsWith("data:image")) {
                String pureBase64 = base64Image.substring(base64Image.indexOf(",") + 1);
                byte[] decodedString = Base64.decode(pureBase64, Base64.DEFAULT);
                Glide.with(this).load(decodedString).centerCrop().into(imgPreview);
            } else {
                // Xử lý hiển thị nếu là link url bình thường
                Glide.with(this).load(base64Image).centerCrop().into(imgPreview);
            }
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
            Court court = new Court(null, name, addr, type, priceVal, base64Image);
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

            if (!base64Image.isEmpty()) updates.put("imageUrl", base64Image);

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