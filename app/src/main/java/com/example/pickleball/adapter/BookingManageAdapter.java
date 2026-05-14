package com.example.pickleball.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.model.Booking;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class BookingManageAdapter extends RecyclerView.Adapter<BookingManageAdapter.ViewHolder> {

    public interface OnActionListener {
        void onConfirm(Booking booking);
        void onReject(Booking booking);
    }

    private final List<Booking> list;
    private final OnActionListener listener;

    public BookingManageAdapter(List<Booking> list, OnActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking_manage, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Booking b = list.get(position);
        Context ctx = h.itemView.getContext();

        h.tvCourtName.setText(b.getCourtName() != null ? b.getCourtName() : "Sân Pickleball");

        // Load tên khách hàng từ Firestore
        h.tvCustomer.setText("👤 Đang tải...");
        if (b.getUserId() != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("Users").document(b.getUserId()).get()
                    .addOnSuccessListener(doc -> {
                        String name = doc.getString("fullName");
                        h.tvCustomer.setText("👤 " + (name != null ? name : "Khách hàng"));
                    })
                    .addOnFailureListener(e ->
                            h.tvCustomer.setText("👤 Khách hàng"));
        } else {
            h.tvCustomer.setText("👤 Khách hàng");
        }

        String dt = (b.getDate() != null ? b.getDate() : "") +
                (b.getStartTime() != null ? "  " + b.getStartTime() + " – " + b.getEndTime() : "");
        h.tvDateTime.setText("📅 " + (dt.trim().isEmpty() ? "Chưa có thông tin" : dt));

        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
        h.tvPrice.setText("💰 " + fmt.format(b.getTotalPrice()) + "đ");

        String status = b.getStatus() != null ? b.getStatus() : "pending";
        switch (status) {
            case "confirmed":
                h.tvStatus.setText("✅ Đã xác nhận");
                h.tvStatus.setTextColor(ctx.getColor(R.color.green_dark));
                h.layoutActions.setVisibility(View.GONE);
                break;
            case "rejected":
                h.tvStatus.setText("❌ Đã từ chối");
                h.tvStatus.setTextColor(ctx.getColor(R.color.error_red));
                h.layoutActions.setVisibility(View.GONE);
                break;
            case "cancelled":
                h.tvStatus.setText("🚫 Đã hủy");
                h.tvStatus.setTextColor(ctx.getColor(R.color.text_tertiary));
                h.layoutActions.setVisibility(View.GONE);
                break;
            default: // pending
                h.tvStatus.setText("⏳ Chờ xác nhận");
                h.tvStatus.setTextColor(ctx.getColor(R.color.warning_yellow));
                h.layoutActions.setVisibility(View.VISIBLE);
                break;
        }

        h.btnConfirm.setOnClickListener(v -> listener.onConfirm(b));
        h.btnReject.setOnClickListener(v -> listener.onReject(b));
    }

    @Override
    public int getItemCount() { return list == null ? 0 : list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCourtName, tvStatus, tvCustomer, tvDateTime, tvPrice;
        LinearLayout layoutActions;
        MaterialButton btnConfirm, btnReject;

        ViewHolder(@NonNull View v) {
            super(v);
            tvCourtName   = v.findViewById(R.id.tvManageCourtName);
            tvStatus      = v.findViewById(R.id.tvManageStatus);
            tvCustomer    = v.findViewById(R.id.tvManageCustomer);
            tvDateTime    = v.findViewById(R.id.tvManageDateTime);
            tvPrice       = v.findViewById(R.id.tvManagePrice);
            layoutActions = v.findViewById(R.id.layoutActions);
            btnConfirm    = v.findViewById(R.id.btnConfirm);
            btnReject     = v.findViewById(R.id.btnReject);
        }
    }
}
