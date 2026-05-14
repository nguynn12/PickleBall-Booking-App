package com.example.pickleball.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pickleball.R;
import com.example.pickleball.model.Court;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class OwnerCourtAdapter extends RecyclerView.Adapter<OwnerCourtAdapter.ViewHolder> {

    public interface OnActionListener {
        void onEdit(Court court);
        void onToggleStatus(Court court);
        void onView(Court court);
    }

    private final Context context;
    private final List<Court> list;
    private final OnActionListener listener;

    public OwnerCourtAdapter(Context context, List<Court> list, OnActionListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_court_owner, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Court court = list.get(position);

        h.tvName.setText(court.getCourtName() != null ? court.getCourtName() : "Sân Pickleball");
        h.tvAddress.setText("📍 " + (court.getAddress() != null ? court.getAddress() : "Chưa có địa chỉ"));
        h.tvHours.setText("🕐 " + court.getOpenHours());

        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
        double price = court.getPricePerHour();
        h.tvPrice.setText(price > 0 ? "💰 " + fmt.format(price) + "đ/giờ" : "💰 Liên hệ");

        // Ảnh
        if (court.getImageUrl() != null && !court.getImageUrl().isEmpty()) {
            Glide.with(context).load(court.getImageUrl()).centerCrop().into(h.imgCourt);
        } else {
            h.imgCourt.setImageResource(0);
            h.imgCourt.setBackgroundColor(context.getColor(R.color.green_light));
        }

        // Trạng thái
        boolean isActive = !"inactive".equals(court.getStatus());
        if (isActive) {
            h.tvStatus.setText("● Đang hoạt động");
            h.tvStatus.setBackgroundResource(R.drawable.bg_badge_green);
            h.btnToggle.setText("⏸ Tạm đóng");
            h.btnToggle.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(context.getColor(R.color.warning_yellow)));
        } else {
            h.tvStatus.setText("● Tạm đóng");
            h.tvStatus.setBackgroundResource(R.drawable.bg_badge_inactive);
            h.btnToggle.setText("▶ Mở lại");
            h.btnToggle.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(context.getColor(R.color.green_primary)));
        }

        h.btnEdit.setOnClickListener(v -> listener.onEdit(court));
        h.btnToggle.setOnClickListener(v -> listener.onToggleStatus(court));
        h.itemView.setOnClickListener(v -> listener.onView(court));
    }

    @Override
    public int getItemCount() { return list == null ? 0 : list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCourt;
        TextView tvName, tvAddress, tvPrice, tvHours, tvStatus;
        MaterialButton btnEdit, btnToggle;

        ViewHolder(@NonNull View v) {
            super(v);
            imgCourt  = v.findViewById(R.id.imgOwnerCourt);
            tvName    = v.findViewById(R.id.tvOwnerCourtName);
            tvAddress = v.findViewById(R.id.tvOwnerCourtAddress);
            tvPrice   = v.findViewById(R.id.tvOwnerCourtPrice);
            tvHours   = v.findViewById(R.id.tvOwnerCourtHours);
            tvStatus  = v.findViewById(R.id.tvCourtStatus);
            btnEdit   = v.findViewById(R.id.btnEditCourt);
            btnToggle = v.findViewById(R.id.btnToggleStatus);
        }
    }
}
