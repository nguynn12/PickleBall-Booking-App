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
        tvEmpty   = view.findViewById(R.id.tvEmpty);
        rvRefunds.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RefundAdapter(refundList, this::onDoneClicked, this::onRejectClicked);
        rvRefunds.setAdapter(adapter);
        loadRefundRequests();
    }

    private void loadRefundRequests() {
        FirebaseFirestore.getInstance()
                .collection("Bookings")
                .whereEqualTo("refundStatus", Constants.REFUND_STATUS_PENDING)
                .addSnapshotListener((snap, err) -> {
                    if (snap == null) return;
                    refundList.clear();
                    for (var doc : snap.getDocuments()) {
                        Booking b = doc.toObject(Booking.class);
                        if (b != null) {
                            if (b.getBookingId() == null) b.setBookingId(doc.getId());
                            refundList.add(b);
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
                        + "đ cho khách " + booking.getCustomerName() + "?")
                .setPositiveButton("Đã hoàn tiền", (d, w) -> updateRefundStatus(booking, Constants.REFUND_STATUS_DONE))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void onRejectClicked(Booking booking) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Từ chối hoàn tiền")
                .setMessage("Từ chối hoàn tiền cho khách " + booking.getCustomerName() + "?")
                .setPositiveButton("Từ chối", (d, w) -> updateRefundStatus(booking, Constants.REFUND_STATUS_REJECTED))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateRefundStatus(Booking booking, String status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("refundStatus", status);
        FirebaseFirestore.getInstance()
                .collection("Bookings").document(booking.getBookingId())
                .update(updates)
                .addOnSuccessListener(v ->
                        Toast.makeText(requireContext(), "Đã cập nhật!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ── Inner adapter ──────────────────────────────────────────────────────────

    interface OnActionCallback { void invoke(Booking b); }

    static class RefundAdapter extends RecyclerView.Adapter<RefundAdapter.VH> {

        private final List<Booking>    list;
        private final OnActionCallback onDone, onReject;

        RefundAdapter(List<Booking> list, OnActionCallback onDone, OnActionCallback onReject) {
            this.list     = list;
            this.onDone   = onDone;
            this.onReject = onReject;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_refund, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Booking b = list.get(position);
            NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));

            h.tvName.setText(b.getCustomerName() != null ? b.getCustomerName() : "—");
            h.tvCourt.setText(b.getCourtName() != null ? b.getCourtName() : "—");
            h.tvDate.setText(b.getDate() != null ? b.getDate() : "—");
            h.tvAmount.setText(fmt.format((long) b.getRefundAmount()) + "đ");
            h.tvReason.setText("Lý do: " + (b.getStatus() != null ? b.getStatus() : "—"));

            h.btnDone.setOnClickListener(v -> onDone.invoke(b));
            h.btnReject.setOnClickListener(v -> onReject.invoke(b));
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvCourt, tvDate, tvAmount, tvReason;
            com.google.android.material.button.MaterialButton btnDone, btnReject;

            VH(@NonNull View v) {
                super(v);
                tvName   = v.findViewById(R.id.tvRefundName);
                tvCourt  = v.findViewById(R.id.tvRefundCourt);
                tvDate   = v.findViewById(R.id.tvRefundDate);
                tvAmount = v.findViewById(R.id.tvRefundAmount);
                tvReason = v.findViewById(R.id.tvRefundReason);
                btnDone  = v.findViewById(R.id.btnRefundDone);
                btnReject = v.findViewById(R.id.btnRefundReject);
            }
        }
    }
}
