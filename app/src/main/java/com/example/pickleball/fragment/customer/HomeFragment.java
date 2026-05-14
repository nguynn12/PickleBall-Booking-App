package com.example.pickleball.fragment.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.activity.court.CourtDetailActivity;
import com.example.pickleball.adapter.CourtAdapter;
import com.example.pickleball.model.Court;
import com.example.pickleball.utils.FirebaseHelper;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private RecyclerView rvCourts;
    private CourtAdapter adapter;
    private final List<Court> masterList  = new ArrayList<>();
    private final List<Court> displayList = new ArrayList<>();
    private EditText edtSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_customer_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Hiển thị ngày hôm nay
        android.widget.TextView tvDate = view.findViewById(R.id.tvDate);
        if (tvDate != null) {
            String today = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN")).format(new Date());
            // Viết hoa chữ đầu
            tvDate.setText(today.substring(0, 1).toUpperCase() + today.substring(1));
        }

        // Nút thông báo
        android.widget.ImageView btnBell = view.findViewById(R.id.btnNotifications);
        if (btnBell != null) {
            btnBell.setOnClickListener(v2 -> startActivity(
                    new Intent(requireContext(), com.example.pickleball.activity.NotificationsActivity.class)));
        }

        rvCourts  = view.findViewById(R.id.rvCourts);
        edtSearch = view.findViewById(R.id.edtSearch);

        rvCourts.setLayoutManager(new LinearLayoutManager(requireContext()));
        // Nút ĐẶT LỊCH → BookingScheduleActivity, click card → CourtDetailActivity
        adapter = new CourtAdapter(requireContext(), displayList, court -> {
            // Click vào card → xem chi tiết
            Intent intent = new Intent(requireContext(), CourtDetailActivity.class);
            intent.putExtra(CourtDetailActivity.EXTRA_COURT, court);
            startActivity(intent);
        });
        rvCourts.setAdapter(adapter);

        // Search
        if (edtSearch != null) {
            edtSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterSearch(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        loadCourts();
        loadUpcomingBooking(view);
    }

    /** Load lịch đặt sắp tới gần nhất của user */
    private void loadUpcomingBooking(View view) {
        // Fragment này không có upcoming card trong layout mới (chỉ có search + list)
        // Upcoming booking hiển thị trong MyBookingsFragment
    }

    private void loadCourts() {
        new FirebaseHelper().getAllCourts(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Toast.makeText(requireContext(), "Không tải được danh sách sân!", Toast.LENGTH_SHORT).show();
                return;
            }
            masterList.clear();
            for (QueryDocumentSnapshot doc : task.getResult()) {
                Court court = doc.toObject(Court.class);
                if (court.getCourtId() == null || court.getCourtId().isEmpty()) {
                    court.setCourtId(doc.getId());
                }
                masterList.add(court);
            }
            displayList.clear();
            displayList.addAll(masterList);
            adapter.notifyDataSetChanged();
        });
    }

    private void filterSearch(String query) {
        displayList.clear();
        if (query == null || query.trim().isEmpty()) {
            displayList.addAll(masterList);
        } else {
            String q = query.toLowerCase();
            for (Court c : masterList) {
                String name = c.getCourtName() == null ? "" : c.getCourtName();
                String addr = c.getAddress() == null ? "" : c.getAddress();
                if (name.toLowerCase().contains(q) || addr.toLowerCase().contains(q)) {
                    displayList.add(c);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}
