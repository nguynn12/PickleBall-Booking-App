package com.example.pickleball.fragment.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.adapter.BookingManageAdapter;
import com.example.pickleball.model.Booking;
import com.example.pickleball.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminFinanceFragment extends Fragment {

    private TextView tvTotalRevenue, tvTotalOrders, tvEmpty;
    private RecyclerView rvFinanceList;
    private TextView tabAll, tabPaid, tabRefund;
    
    private final List<Booking> allBookings = new ArrayList<>();
    private final List<Booking> displayList = new ArrayList<>();
    
    private BookingManageAdapter bookingAdapter;
    private AdminRefundFragment.RefundAdapter refundAdapter;
    
    private String currentFilter = "all"; // "all", "paid", "refund"
    private final NumberFormat currencyFmt = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_finance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTotalRevenue = view.findViewById(R.id.tvTotalRevenue);
        tvTotalOrders = view.findViewById(R.id.tvTotalOrders);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        rvFinanceList = view.findViewById(R.id.rvFinanceList);
        tabAll = view.findViewById(R.id.tabAll);
        tabPaid = view.findViewById(R.id.tabPaid);
        tabRefund = view.findViewById(R.id.tabRefund);

        rvFinanceList.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Adapter cho các tab hiển thị booking chung (Tất cả, Đã thanh toán)
        bookingAdapter = new BookingManageAdapter(displayList, new BookingManageAdapter.OnActionListener() {
            @Override public void onConfirm(Booking b) {
                // Admin không phê duyệt đơn ở đây, nhưng để trống listener để tránh crash
            }
            @Override public void onReject(Booking b) {}
        });
        
        // Adapter riêng cho tab Hoàn tiền vì có nút "Đã hoàn tiền"
        refundAdapter = new AdminRefundFragment.RefundAdapter(displayList, this::onDoneClicked, this::onRejectClicked);

        tabAll.setOnClickListener(v -> applyFilter("all", tabAll, tabPaid, tabRefund));
        tabPaid.setOnClickListener(v -> applyFilter("paid", tabPaid, tabAll, tabRefund));
        tabRefund.setOnClickListener(v -> applyFilter("refund", tabRefund, tabAll, tabPaid));

        // Mặc định ban đầu
        applyFilter("all", tabAll, tabPaid, tabRefund);

        loadAllData();
    }

    private void applyFilter(String filter, TextView selected, TextView u1, TextView u2) {
        currentFilter = filter;
        setTabSelected(selected);
        setTabUnselected(u1);
        setTabUnselected(u2);
        
        // Thay đổi adapter tùy theo tab
        if ("refund".equals(filter)) {
            rvFinanceList.setAdapter(refundAdapter);
        } else {
            rvFinanceList.setAdapter(bookingAdapter);
        }
        
        filterAndShow();
    }

    private void loadAllData() {
        // Lấy toàn bộ Bookings để tính toán doanh thu và lọc
        FirebaseFirestore.getInstance().collection("Bookings")
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    if (snap == null) return;
                    
                    allBookings.clear();
                    double totalRev = 0;
                    int paidCount = 0;

                    for (QueryDocumentSnapshot doc : snap) {
                        Booking b = doc.toObject(Booking.class);
                        if (b != null) {
                            if (b.getBookingId() == null) b.setBookingId(doc.getId());
                            allBookings.add(b);
                            
                            // Chỉ tính doanh thu từ tiền cọc của các đơn đã thanh toán (paid)
                            if (Constants.PAYMENT_STATUS_PAID.equals(b.getPaymentStatus())) {
                                totalRev += b.getDepositAmount();
                                paidCount++;
                            }
                        }
                    }
                    
                    // Cập nhật các con số thống kê ở header
                    tvTotalRevenue.setText(currencyFmt.format((long) totalRev) + "đ");
                    tvTotalOrders.setText(String.valueOf(paidCount));
                    
                    // Cập nhật danh sách hiển thị
                    filterAndShow();
                });
    }

    private void filterAndShow() {
        displayList.clear();
        for (Booking b : allBookings) {
            switch (currentFilter) {
                case "all":
                    displayList.add(b);
                    break;
                case "paid":
                    if (Constants.PAYMENT_STATUS_PAID.equals(b.getPaymentStatus())) {
                        displayList.add(b);
                    }
                    break;
                case "refund":
                    if (Constants.REFUND_STATUS_PENDING.equals(b.getRefundStatus())) {
                        displayList.add(b);
                    }
                    break;
            }
        }
        
        RecyclerView.Adapter<?> adapter = rvFinanceList.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        
        tvEmpty.setVisibility(displayList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onDoneClicked(Booking booking) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận hoàn tiền")
                .setMessage("Bạn xác nhận đã chuyển khoản hoàn lại " + currencyFmt.format((long) booking.getRefundAmount()) + "đ cho khách hàng?")
                .setPositiveButton("Xác nhận", (d, w) -> updateRefundStatus(booking, Constants.REFUND_STATUS_DONE))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void onRejectClicked(Booking booking) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Từ chối hoàn tiền")
                .setMessage("Bạn chắc chắn muốn từ chối yêu cầu hoàn tiền này?")
                .setPositiveButton("Từ chối", (d, w) -> updateRefundStatus(booking, Constants.REFUND_STATUS_REJECTED))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateRefundStatus(Booking booking, String status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("refundStatus", status);
        
        FirebaseFirestore.getInstance().collection("Bookings").document(booking.getBookingId())
                .update(updates)
                .addOnSuccessListener(v -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Đã cập nhật trạng thái hoàn tiền!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setTabSelected(TextView tab) {
        tab.setBackgroundResource(R.drawable.bg_chip_selected);
        tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_primary));
    }

    private void setTabUnselected(TextView tab) {
        tab.setBackgroundResource(R.drawable.bg_chip_unselected);
        tab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
    }
}
