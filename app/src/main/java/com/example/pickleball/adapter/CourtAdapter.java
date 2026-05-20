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

public class CourtAdapter extends RecyclerView.Adapter<CourtAdapter.CourtViewHolder> {

    public interface OnCourtClickListener {
        void onCourtClick(Court court);
    }

    private final Context context;
    private final List<Court> courtList;
    private final OnCourtClickListener clickListener;

    public CourtAdapter(Context context, List<Court> courtList, OnCourtClickListener clickListener) {
        this.context = context;
        this.courtList = courtList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public CourtViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_court, parent, false);
        return new CourtViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourtViewHolder holder, int position) {
        Court court = courtList.get(position);

        String name     = court.getCourtName();
        String address  = court.getAddress();
        double price    = court.getPricePerHour();
        String imageUrl = court.getImageUrl();

        // Tên sân
        holder.tvCourtNameItem.setText(
                name != null && !name.isEmpty() ? name : "Sân Pickleball");

        // Địa chỉ (màu xanh, giống ảnh mẫu)
        holder.tvLocationItem.setText(
                address != null && !address.isEmpty() ? address : "Chưa có địa chỉ");

        // Giờ mở cửa từ model
        holder.tvOpenHours.setText("🕐 " + court.getOpenHours());

        // Rating từ Firestore
        double avg = court.getAvgRating();
        holder.tvRating.setText(avg > 0
                ? String.format(java.util.Locale.getDefault(), "⭐ %.1f", avg)
                : "⭐ Mới");

        // Ảnh sân
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.color.green_light)
                    .error(R.color.green_light)
                    .centerCrop()
                    .into(holder.imgCourtItem);
            // Logo cũng dùng ảnh sân
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_nav_court)
                    .circleCrop()
                    .into(holder.imgCourtLogo);
        } else {
            holder.imgCourtItem.setImageResource(0);
            holder.imgCourtItem.setBackgroundColor(
                    context.getResources().getColor(R.color.green_light, null));
            holder.imgCourtLogo.setImageResource(R.drawable.ic_nav_court);
        }

        // Nút ĐẶT LỊCH
        holder.btnBookItem.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onCourtClick(court);
        });
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onCourtClick(court);
        });
    }

    @Override
    public int getItemCount() {
        return courtList == null ? 0 : courtList.size();
    }

    public static class CourtViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCourtItem, imgCourtLogo;
        TextView tvCourtNameItem, tvLocationItem, tvOpenHours, tvRating;
        MaterialButton btnBookItem;

        public CourtViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCourtItem    = itemView.findViewById(R.id.imgCourtItem);
            imgCourtLogo    = itemView.findViewById(R.id.imgCourtLogo);
            tvCourtNameItem = itemView.findViewById(R.id.tvCourtNameItem);
            tvLocationItem  = itemView.findViewById(R.id.tvLocationItem);
            tvOpenHours     = itemView.findViewById(R.id.tvOpenHours);
            tvRating        = itemView.findViewById(R.id.tvRating);
            btnBookItem     = itemView.findViewById(R.id.btnBookItem);
        }
    }
}
