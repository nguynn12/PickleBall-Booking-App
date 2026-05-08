package com.example.pickleball.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pickleball.R;
import com.example.pickleball.model.Court;

import java.util.List;

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

        // Handle null safely
        String name = court.getCourtName();
        String address = court.getAddress();
        double price = court.getPricePerHour();
        String imageUrl = court.getImageUrl();

        holder.tvCourtNameItem.setText(name != null && !name.isEmpty() ? name : "Sân Pickleball");
        holder.tvLocationItem.setText("📍 " + (address != null && !address.isEmpty() ? address : "Chưa có địa chỉ"));
        holder.tvPriceItem.setText(formatPrice(price));

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_nav_court)
                    .error(R.drawable.ic_nav_court)
                    .centerCrop()
                    .into(holder.imgCourtItem);
        } else {
            holder.imgCourtItem.setImageResource(R.drawable.ic_nav_court);
            holder.imgCourtItem.setBackgroundColor(
                    context.getResources().getColor(R.color.green_light, null));
        }

        holder.btnBookItem.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onCourtClick(court);
        });
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onCourtClick(court);
        });
    }

    private String formatPrice(double price) {
        if (price <= 0) return "Liên hệ";
        java.text.NumberFormat fmt = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
        return fmt.format(price) + "đ/h";
    }

    @Override
    public int getItemCount() {
        return courtList.size();
    }

    public static class CourtViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCourtItem;
        TextView tvCourtNameItem, tvPriceItem, tvLocationItem;
        Button btnBookItem;

        public CourtViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCourtItem = itemView.findViewById(R.id.imgCourtItem);
            tvCourtNameItem = itemView.findViewById(R.id.tvCourtNameItem);
            tvPriceItem = itemView.findViewById(R.id.tvPriceItem);
            tvLocationItem = itemView.findViewById(R.id.tvLocationItem);
            btnBookItem = itemView.findViewById(R.id.btnBookItem);
        }
    }
}
