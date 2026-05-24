package com.example.pickleball.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.activity.auth.LoginActivity;
import com.example.pickleball.activity.court.CourtDetailActivity;
import com.example.pickleball.adapter.CourtAdapter;
import com.example.pickleball.model.Court;
import com.example.pickleball.utils.FirebaseHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeActivity extends AppCompatActivity {
    private final List<String> favoriteIds = new ArrayList<>();
    private RecyclerView rvCourts;
    private CourtAdapter adapter;
    private final List<Court> masterCourtList = new ArrayList<>();
    private final List<Court> displayList = new ArrayList<>();

    private TextView btnFilterNearMe, btnFilterAvailable, btnFilterIndoor;
    private EditText edtSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        rvCourts = findViewById(R.id.rvCourts);
        btnFilterNearMe = findViewById(R.id.btnFilterNearMe);
        btnFilterAvailable = findViewById(R.id.btnFilterAvailable);
        btnFilterIndoor = findViewById(R.id.btnFilterIndoor);
        edtSearch = findViewById(R.id.edtSearch);

        CircleImageView imgAvatar = findViewById(R.id.imgAvatar);
        if (imgAvatar != null) {
            imgAvatar.setOnClickListener(v -> showAccountBottomSheet());
        }

        TextView btnAdminMenu = findViewById(R.id.btnAdminMenu);
        if (btnAdminMenu != null) {
            btnAdminMenu.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, AdminDashboardActivity.class);
                startActivity(intent);
            });
        }

        rvCourts.setLayoutManager(new LinearLayoutManager(this));
        // Thêm biến favoriteIds vào tham số thứ 3
        adapter = new CourtAdapter(this, displayList, favoriteIds, court -> {
            Intent intent = new Intent(this, CourtDetailActivity.class);
            intent.putExtra(CourtDetailActivity.EXTRA_COURT, court);
            startActivity(intent);
        });
        rvCourts.setAdapter(adapter);

        if (edtSearch != null) {
            edtSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterSearch(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (btnFilterNearMe != null) {
            btnFilterNearMe.setOnClickListener(v -> {
                filterAll();
                updateFilterUI(btnFilterNearMe, btnFilterAvailable, btnFilterIndoor);
                clearSearch();
            });
        }

        if (btnFilterAvailable != null) {
            btnFilterAvailable.setOnClickListener(v -> {
                filterAvailable();
                updateFilterUI(btnFilterAvailable, btnFilterNearMe, btnFilterIndoor);
                clearSearch();
            });
        }

        if (btnFilterIndoor != null) {
            btnFilterIndoor.setOnClickListener(v -> {
                filterIndoor();
                updateFilterUI(btnFilterIndoor, btnFilterNearMe, btnFilterAvailable);
                clearSearch();
            });
        }

        loadCourtsFromFirestore();
    }

    private void loadCourtsFromFirestore() {
        FirebaseHelper helper = new FirebaseHelper();
        helper.getAllCourts(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Toast.makeText(HomeActivity.this, "Không tải được danh sách sân!", Toast.LENGTH_SHORT).show();
                return;
            }

            masterCourtList.clear();
            for (QueryDocumentSnapshot doc : task.getResult()) {
                Court court = doc.toObject(Court.class);
                if (court.getCourtId() == null || court.getCourtId().isEmpty()) {
                    court.setCourtId(doc.getId());
                }
                masterCourtList.add(court);
            }

            filterAll();
            updateFilterUI(btnFilterNearMe, btnFilterAvailable, btnFilterIndoor);
        });
    }

    private void filterSearch(String query) {
        displayList.clear();

        if (query == null || query.trim().isEmpty()) {
            filterAll();
            updateFilterUI(btnFilterNearMe, btnFilterAvailable, btnFilterIndoor);
            return;
        }

        String lowerCaseQuery = query.toLowerCase();
        for (Court c : masterCourtList) {
            String name = c.getCourtName() == null ? "" : c.getCourtName();
            String address = c.getAddress() == null ? "" : c.getAddress();
            if (name.toLowerCase().contains(lowerCaseQuery) || address.toLowerCase().contains(lowerCaseQuery)) {
                displayList.add(c);
            }
        }

        adapter.notifyDataSetChanged();

        if (btnFilterNearMe != null) {
            btnFilterNearMe.setBackgroundResource(R.drawable.bg_search_bar);
            btnFilterNearMe.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }
        if (btnFilterAvailable != null) {
            btnFilterAvailable.setBackgroundResource(R.drawable.bg_search_bar);
            btnFilterAvailable.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }
        if (btnFilterIndoor != null) {
            btnFilterIndoor.setBackgroundResource(R.drawable.bg_search_bar);
            btnFilterIndoor.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }
    }

    private void clearSearch() {
        if (edtSearch != null && !edtSearch.getText().toString().isEmpty()) {
            edtSearch.setText("");
            edtSearch.clearFocus();
        }
    }

    private void filterAll() {
        displayList.clear();
        displayList.addAll(masterCourtList);
        adapter.notifyDataSetChanged();
    }

    private void filterAvailable() {
        displayList.clear();
        for (Court c : masterCourtList) {
            if (c.getPricePerHour() > 0) {
                displayList.add(c);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void filterIndoor() {
        displayList.clear();
        for (Court c : masterCourtList) {
            String type = c.getType() == null ? "" : c.getType();
            if (type.toLowerCase().contains("trong")) {
                displayList.add(c);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void updateFilterUI(TextView selected, TextView u1, TextView u2) {
        if (selected != null) {
            // Khi chọn: Cho nền màu xanh (đã có trong drawable của bạn), chữ màu trắng
            selected.setBackgroundResource(R.drawable.bg_button_primary);
            selected.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
        if (u1 != null) {
            // Khi không chọn: Nền trắng xám, chữ màu đen/xám đậm
            u1.setBackgroundResource(R.drawable.bg_search_bar);
            u1.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }
        if (u2 != null) {
            u2.setBackgroundResource(R.drawable.bg_search_bar);
            u2.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }
    }

    private void showAccountBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_account, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        ImageView btnCloseSheet = bottomSheetView.findViewById(R.id.btnCloseSheet);
        if (btnCloseSheet != null) {
            btnCloseSheet.setOnClickListener(v -> bottomSheetDialog.dismiss());
        }

        View btnLogin = bottomSheetView.findViewById(R.id.btnLogin);
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                startActivity(intent);
            });
        }

        bottomSheetDialog.show();
    }

    public static String formatVnd(double value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return format.format(value);
    }
}
