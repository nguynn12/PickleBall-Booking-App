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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.model.Booking;
import com.example.pickleball.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminRefundFragment extends Fragment {

    private RecyclerView rvRefunds;
    private TextView tvEmpty;
    private final List<Booking> refundList = new ArrayList<>();
    private RefundAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_refund, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvRefunds = view.findViewById(R.id.rvRefunds);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        rvRefunds.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RefundAdapter(refundList, this::onDoneClicked, this::onRejectClicked);
        rvRefunds.setAdapter(adapter);

        loadRefundRequests();
    }

    private void loadRefundRequests() {
        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_BOOKINGS)
                .whereEqualTo("refundStatus", Constants.REFUND_STATUS_PENDING)
                .addSnapshotListener((snap, err) -> {
                    if (snap == null) return;

                    refundList.clear();
                    for (var doc : snap.getDocuments()) {
                        Booking booking = doc.toObject(Booking.class);
                        if (booking != null) {
                            if (booking.getBookingId() == null) booking.setBookingId(doc.getId());
                            refundList.add(booking);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(refundList.isEmpty() ? View.VISIBLE : View.GONE);
                    rvRefunds.setVisibility(refundList.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void onDoneClicked(Booking booking) {
        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận đã hoàn tiền")
                .setMessage("Xác nhận đã hoàn " + fmt.format((long) booking.getRefundAmount())
                        + "đ cho khách " + safeText(booking.getCustomerName(), "Khách hàng") + "?")
                .setPositiveButton("Đã hoàn tiền", (d, w) -> updateRefundStatus(booking, Constants.REFUND_STATUS_DONE))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void onRejectClicked(Booking booking) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Từ chối hoàn tiền")
                .setMessage("Từ chối hoàn tiền cho khách " + safeText(booking.getCustomerName(), "Khách hàng") + "?")
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
                .addOnSuccessListener(v ->
                        Toast.makeText(requireContext(), "Đã cập nhật!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private static String safeText(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value : fallback;
    }

    interface OnActionCallback {
        void invoke(Booking booking);
    }

    static class RefundAdapter extends RecyclerView.Adapter<RefundAdapter.VH> {

        private final List<Booking> list;
        private final OnActionCallback onDone, onReject;

        RefundAdapter(List<Booking> list, OnActionCallback onDone, OnActionCallback onReject) {
            this.list = list;
            this.onDone = onDone;
            this.onReject = onReject;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_refund, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Booking booking = list.get(position);
            NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));

            holder.tvName.setText(safeText(booking.getCustomerName(), "Khách hàng"));
            holder.tvCourt.setText(safeText(booking.getCourtName(), "Sân Pickleball"));
            holder.tvDate.setText(safeText(booking.getDate(), "-"));
            holder.tvAmount.setText(fmt.format((long) booking.getRefundAmount()) + "đ");
            holder.tvReason.setText("Lý do: " + safeText(booking.getCancelReason(), "Không có ghi chú"));

            holder.btnDone.setOnClickListener(v -> onDone.invoke(booking));
            holder.btnReject.setOnClickListener(v -> onReject.invoke(booking));
        }

        @Override
        public int getItemCount() {
            return list == null ? 0 : list.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvCourt, tvDate, tvAmount, tvReason;
            com.google.android.material.button.MaterialButton btnDone, btnReject;

            VH(@NonNull View view) {
                super(view);
                tvName = view.findViewById(R.id.tvRefundName);
                tvCourt = view.findViewById(R.id.tvRefundCourt);
                tvDate = view.findViewById(R.id.tvRefundDate);
                tvAmount = view.findViewById(R.id.tvRefundAmount);
                tvReason = view.findViewById(R.id.tvRefundReason);
                btnDone = view.findViewById(R.id.btnRefundDone);
                btnReject = view.findViewById(R.id.btnRefundReject);
            }
        }
    }
}
