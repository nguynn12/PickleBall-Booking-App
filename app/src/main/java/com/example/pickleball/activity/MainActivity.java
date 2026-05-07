package com.example.pickleball.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Import thư viện Firestore
import com.example.pickleball.R;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- CODE TEST GHI DỮ LIỆU LÊN FIRESTORE ---

        // 1. Khởi tạo kết nối với Cloud Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 2. Tạo một đối tượng dữ liệu mẫu (Ví dụ: 1 Sân Pickleball)
        Map<String, Object> testCourt = new HashMap<>();
        testCourt.put("court_name", "Sân Pickleball Chuyên Nghiệp Số 1");
        testCourt.put("type", "Trong nhà");
        testCourt.put("price_per_hour", 150000);

        // 3. Đẩy lên Firestore vào một collection (bảng) tên là "Courts"
        db.collection("Courts").add(testCourt)
                .addOnSuccessListener(documentReference -> {
                    // Nếu ghi thành công, báo Toast trên màn hình điện thoại
                    Toast.makeText(MainActivity.this, "Đã lưu sân thành công lên Cloud Firestore!", Toast.LENGTH_LONG).show();
                    Log.d("FirebaseTest", "Ghi thành công với ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    // Nếu thất bại, ghi lỗi ra cửa sổ Logcat
                    Log.w("FirebaseTest", "Lỗi khi lưu dữ liệu", e);
                });
    }
}