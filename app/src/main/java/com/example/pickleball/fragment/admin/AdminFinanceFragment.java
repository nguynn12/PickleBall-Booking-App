package com.example.pickleball.fragment.admin;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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

    private FinanceBookingAdapter bookingAdapter;
    private AdminRefundFragment.RefundAdapter refundAdapter;

    private String currentFilter = "all";
    private final NumberFormat currencyFmt = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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
        bookingAdapter = new FinanceBookingAdapter(displayList);
        refundAdapter = new AdminRefundFragment.RefundAdapter(displayList, this::onDoneClicked, this::onRejectClicked);

        tabAll.setOnClickListener(v -> applyFilter("all", tabAll, tabPaid, tabRefund));
        tabPaid.setOnClickListener(v -> applyFilter("paid", tabPaid, tabAll, tabRefund));
        tabRefund.setOnClickListener(v -> applyFilter("refund", tabRefund, tabAll, tabPaid));

        applyFilter("all", tabAll, tabPaid, tabRefund);
        loadAllData();
    }

    private void applyFilter(String filter, TextView selected, TextView u1, TextView u2) {
        currentFilter = filter;
        setTabSelected(selected);
        setTabUnselected(u1);
        setTabUnselected(u2);

        rvFinanceList.setAdapter("refund".equals(filter) ? refundAdapter : bookingAdapter);
        filterAndShow();
    }

    private void loadAllData() {
        FirebaseFirestore.getInstance().collection(Constants.COLLECTION_BOOKINGS)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    if (snap == null) return;

                    allBookings.clear();
                    double totalRevenue = 0;
                    int paidCount = 0;

                    for (QueryDocumentSnapshot doc : snap) {
                        Booking booking = doc.toObject(Booking.class);
                        if (booking != null) {
                            if (booking.getBookingId() == null) booking.setBookingId(doc.getId());
                            allBookings.add(booking);

                            if (Constants.PAYMENT_STATUS_PAID.equals(booking.getPaymentStatus())) {
                                totalRevenue += booking.getDepositAmount();
                                paidCount++;
                            }
                        }
                    }

                    tvTotalRevenue.setText(currencyFmt.format((long) totalRevenue) + "đ");
                    tvTotalOrders.setText(String.valueOf(paidCount));
                    filterAndShow();
                });
    }

    private void filterAndShow() {
        displayList.clear();
        for (Booking booking : allBookings) {
            switch (currentFilter) {
                case "paid":
                    if (Constants.PAYMENT_STATUS_PAID.equals(booking.getPaymentStatus())) {
                        displayList.add(booking);
                    }
                    break;
                case "refund":
                    if (Constants.REFUND_STATUS_PENDING.equals(booking.getRefundStatus())) {
                        displayList.add(booking);
                    }
                    break;
                default:
                    displayList.add(booking);
                    break;
            }
        }

        RecyclerView.Adapter<?> adapter = rvFinanceList.getAdapter();
        if (adapter != null) adapter.notifyDataSetChanged();

        tvEmpty.setVisibility(displayList.isEmpty() ? View.VISIBLE : View.GONE);
        rvFinanceList.setVisibility(displayList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void onDoneClicked(Booking booking) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận hoàn tiền")
                .setMessage("Bạn xác nhận đã chuyển khoản hoàn lại "
                        + currencyFmt.format((long) booking.getRefundAmount()) + "đ cho khách hàng?")
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

        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_BOOKINGS)
                .document(booking.getBookingId())
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
        tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
    }

    private static class FinanceBookingAdapter extends RecyclerView.Adapter<FinanceBookingAdapter.VH> {
        private final List<Booking> list;
        private final NumberFormat currencyFmt = NumberFormat.getInstance(new Locale("vi", "VN"));

        FinanceBookingAdapter(List<Booking> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_booking_manage, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Booking booking = list.get(position);
            Context context = holder.itemView.getContext();

            holder.tvCourtName.setText(booking.getCourtName() != null ? booking.getCourtName() : "Sân Pickleball");
            holder.tvCustomer.setText("Khách: " + safeText(booking.getCustomerName(), "Khách hàng"));

            String dateTime = safeText(booking.getDate(), "")
                    + (booking.getStartTime() != null ? "  " + booking.getStartTime() + " - " + safeText(booking.getEndTime(), "") : "");
            holder.tvDateTime.setText(dateTime.trim().isEmpty() ? "Chưa có thông tin" : dateTime);

            holder.tvPrice.setText(currencyFmt.format((long) booking.getDepositAmount()) + "đ cọc");
            bindStatus(holder.tvStatus, booking, context);
            holder.layoutActions.setVisibility(View.GONE);
        }

        private void bindStatus(TextView tvStatus, Booking booking, Context context) {
            String paymentStatus = booking.getPaymentStatus();
            if (Constants.PAYMENT_STATUS_PAID.equals(paymentStatus)) {
                tvStatus.setText("Đã thanh toán");
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green_primary));
            } else if (Constants.PAYMENT_STATUS_REFUNDED.equals(paymentStatus)) {
                tvStatus.setText("Đã hoàn tiền");
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.text_tertiary));
            } else if (Constants.PAYMENT_STATUS_EXPIRED.equals(paymentStatus)) {
                tvStatus.setText("Hết hạn thanh toán");
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.error_red));
            } else {
                tvStatus.setText("Chờ thanh toán");
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.warning_yellow));
            }
        }

        private String safeText(String value, String fallback) {
            return value != null && !value.trim().isEmpty() ? value : fallback;
        }

        @Override
        public int getItemCount() {
            return list == null ? 0 : list.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvCourtName, tvStatus, tvCustomer, tvDateTime, tvPrice;
            LinearLayout layoutActions;

            VH(@NonNull View view) {
                super(view);
                tvCourtName = view.findViewById(R.id.tvManageCourtName);
                tvStatus = view.findViewById(R.id.tvManageStatus);
                tvCustomer = view.findViewById(R.id.tvManageCustomer);
                tvDateTime = view.findViewById(R.id.tvManageDateTime);
                tvPrice = view.findViewById(R.id.tvManagePrice);
                layoutActions = view.findViewById(R.id.layoutActions);
            }
        }
    }
}
