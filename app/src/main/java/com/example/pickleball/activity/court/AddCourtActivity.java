package com.example.pickleball.activity.court;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;import androidx.appcompat.app.AppCompatActivity;

import com.example.pickleball.R;
import com.example.pickleball.model.Court;
import com.example.pickleball.utils.FirebaseHelper;

public class AddCourtActivity extends AppCompatActivity {
    private EditText edtName, edtAddress, edtPrice;
    private Button btnSave;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_court);

        // Khởi tạo views
        edtName = findViewById(R.id.edtCourtName);
        edtAddress = findViewById(R.id.edtAddress);
        edtPrice = findViewById(R.id.edtPrice);
        btnSave = findViewById(R.id.btnSave);

        // Khởi tạo Helper
        firebaseHelper = new FirebaseHelper();

        btnSave.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String address = edtAddress.getText().toString().trim();
            String priceStr = edtPrice.getText().toString().trim();

            if (name.isEmpty() || address.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double price = Double.parseDouble(priceStr);

                // Fix: Truyền đủ 6 tham số cho constructor của Court
                // Tạm thời để type là "Trong nhà" và imageUrl là rỗng do layout chưa có các trường này
                Court newCourt = new Court(null, name, address, "Trong nhà", price, "");

                // Gọi hàm từ FirebaseHelper
                firebaseHelper.addCourt(newCourt,
                        aVoid -> {
                            Toast.makeText(this, "Thêm sân thành công!", Toast.LENGTH_SHORT).show();
                            finish(); // Đóng màn hình
                        },
                        e -> {
                            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                );
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Giá tiền không hợp lệ!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
