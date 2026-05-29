package com.example.pickleball.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pickleball.R;
import com.example.pickleball.model.Court;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class CourtAdapter extends RecyclerView.Adapter<CourtAdapter.CourtViewHolder> {

    public interface OnCourtClickListener {
        void onCourtClick(Court court);
    }

    private final Context context;
    private final List<Court> courtList;
    private final List<String> favoriteList; // Danh sách ID các sân đã thích
    private final OnCourtClickListener clickListener;

    // Constructor cập nhật để nhận thêm danh sách yêu thích
    public CourtAdapter(Context context, List<Court> courtList, List<String> favoriteList, OnCourtClickListener clickListener) {
        this.context = context;
        this.courtList = courtList;
        this.favoriteList = favoriteList;
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
        String imageUrl = court.getImageUrl();

        holder.tvCourtNameItem.setText(name != null && !name.isEmpty() ? name : "Sân Pickleball");
        holder.tvLocationItem.setText(address != null && !address.isEmpty() ? address : "Chưa có địa chỉ");
        holder.tvOpenHours.setText("🕐 " + court.getOpenHours());

        // Rating từ Firestore
        double avg = court.getAvgRating();
        holder.tvRating.setText(avg > 0
                ? String.format(java.util.Locale.getDefault(), "★ %.1f", avg)
                : "★ Mới");

        // --- XỬ LÝ MÀU TRÁI TIM ---
        // Kiểm tra nếu ID của sân này nằm trong danh sách yêu thích thì tô đỏ
        if (favoriteList != null && favoriteList.contains(court.getCourtId())) {
            holder.btnFavoriteItem.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark));
        } else {
            // Nếu không thì để màu xám mặc định (mã màu #BDBDBD như trong file XML của bạn)
            holder.btnFavoriteItem.setColorFilter(ContextCompat.getColor(context, R.color.text_tertiary));
        }

        // --- HIỂN THỊ ẢNH ---
        if (imageUrl != null && !imageUrl.isEmpty()) {
            if (imageUrl.startsWith("data:image")) {
                String pureBase64 = imageUrl.substring(imageUrl.indexOf(",") + 1);
                byte[] decodedString = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT);
                Glide.with(context).load(decodedString).centerCrop().into(holder.imgCourtItem);
                Glide.with(context).load(decodedString).circleCrop().into(holder.imgCourtLogo);
            } else {
                Glide.with(context).load(imageUrl).placeholder(R.color.green_light).centerCrop().into(holder.imgCourtItem);
                Glide.with(context).load(imageUrl).placeholder(R.drawable.ic_nav_court).circleCrop().into(holder.imgCourtLogo);
            }
        } else {
            holder.imgCourtItem.setImageResource(0);
            holder.imgCourtItem.setBackgroundColor(context.getResources().getColor(R.color.green_light, null));
            holder.imgCourtLogo.setImageResource(R.drawable.ic_nav_court);
        }

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
        ImageView imgCourtItem, imgCourtLogo, btnFavoriteItem;
        TextView tvCourtNameItem, tvLocationItem, tvOpenHours, tvRating;
        MaterialButton btnBookItem;

        public CourtViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCourtItem    = itemView.findViewById(R.id.imgCourtItem);
            imgCourtLogo    = itemView.findViewById(R.id.imgCourtLogo);

            // Đã sửa lại đúng ID "btnFavorite" khớp với file item_court.xml của bạn
            btnFavoriteItem = itemView.findViewById(R.id.btnFavorite);

            tvCourtNameItem = itemView.findViewById(R.id.tvCourtNameItem);
            tvLocationItem  = itemView.findViewById(R.id.tvLocationItem);
            tvOpenHours     = itemView.findViewById(R.id.tvOpenHours);
            tvRating        = itemView.findViewById(R.id.tvRating);
            btnBookItem     = itemView.findViewById(R.id.btnBookItem);
        }
    }
}
