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
import com.example.pickleball.model.Court;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AdminCourtAdapter extends RecyclerView.Adapter<AdminCourtAdapter.ViewHolder> {

    public interface OnActionListener {
        void onActivate(Court court);
        void onDeactivate(Court court);
        void onView(Court court);
    }

    private final List<Court> list;
    private final OnActionListener listener;

    public AdminCourtAdapter(List<Court> list, OnActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_court_admin, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Court court = list.get(position);
        Context ctx = h.itemView.getContext();

        h.tvName.setText(court.getCourtName() != null ? court.getCourtName() : "Sân Pickleball");
        h.tvAddress.setText("📍 " + (court.getAddress() != null ? court.getAddress() : "Chưa có địa chỉ"));
        h.tvType.setText(court.getType() != null ? court.getType() : "Pickleball");

        // Load tên chủ sân từ Firestore
        String ownerId = court.getOwnerId();
        h.tvOwner.setText("👤 Chủ sân: Đang tải...");
        if (ownerId != null && !ownerId.isEmpty()) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("Users").document(ownerId).get()
                    .addOnSuccessListener(doc -> {
                        String name = doc.getString("fullName");
                        h.tvOwner.setText("👤 Chủ sân: " + (name != null ? name : "Chưa rõ"));
                    })
                    .addOnFailureListener(e -> h.tvOwner.setText("👤 Chủ sân: Chưa rõ"));
        } else {
            h.tvOwner.setText("👤 Chủ sân: Chưa rõ");
        }

        // Giá
        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
        double price = court.getPricePerHour();
        h.tvPrice.setText(price > 0 ? "💰 " + fmt.format(price) + "đ/giờ" : "💰 Liên hệ");

        // Trạng thái
        boolean isActive = !"inactive".equals(court.getStatus());
        if (isActive) {
            h.tvStatus.setText("Đang hoạt động");
            h.tvStatus.setBackgroundResource(R.drawable.bg_badge_green);
            h.btnActivate.setVisibility(View.GONE);
            h.btnDeactivate.setVisibility(View.VISIBLE);
        } else {
            h.tvStatus.setText("Đã vô hiệu hóa");
            h.tvStatus.setBackgroundResource(R.drawable.bg_badge_inactive);
            h.btnActivate.setVisibility(View.VISIBLE);
            h.btnDeactivate.setVisibility(View.GONE);
        }
        h.layoutActions.setVisibility(View.VISIBLE);

        h.btnActivate.setOnClickListener(v -> listener.onActivate(court));
        h.btnDeactivate.setOnClickListener(v -> listener.onDeactivate(court));
        h.itemView.setOnClickListener(v -> listener.onView(court));
    }

    @Override
    public int getItemCount() { return list == null ? 0 : list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus, tvOwner, tvAddress, tvPrice, tvType;
        LinearLayout layoutActions;
        MaterialButton btnActivate, btnDeactivate;

        ViewHolder(@NonNull View v) {
            super(v);
            tvName        = v.findViewById(R.id.tvAdminCourtName);
            tvStatus      = v.findViewById(R.id.tvAdminCourtStatus);
            tvOwner       = v.findViewById(R.id.tvAdminCourtOwner);
            tvAddress     = v.findViewById(R.id.tvAdminCourtAddress);
            tvPrice       = v.findViewById(R.id.tvAdminCourtPrice);
            tvType        = v.findViewById(R.id.tvAdminCourtType);
            layoutActions = v.findViewById(R.id.layoutAdminActions);
            btnActivate   = v.findViewById(R.id.btnAdminActivate);
            btnDeactivate = v.findViewById(R.id.btnAdminDeactivate);
        }
    }
}
