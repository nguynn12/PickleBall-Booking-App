package com.example.pickleball.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.model.Review;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private final List<Review> list;

    public ReviewAdapter(List<Review> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Review r = list.get(position);

        String name = r.getUserName() != null ? r.getUserName() : "Ẩn danh";
        h.tvName.setText(name);
        h.tvInitial.setText(name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase());
        h.tvComment.setText(r.getComment() != null ? r.getComment() : "");

        // Rating
        h.tvRating.setText(String.format(Locale.getDefault(), "⭐ %.1f", r.getRating()));

        // Ngày
        if (r.getCreatedAt() > 0) {
            String date = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"))
                    .format(new Date(r.getCreatedAt()));
            h.tvDate.setText(date);
        } else {
            h.tvDate.setText("");
        }
    }

    @Override
    public int getItemCount() { return list == null ? 0 : list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitial, tvName, tvDate, tvRating, tvComment;

        ViewHolder(@NonNull View v) {
            super(v);
            tvInitial = v.findViewById(R.id.tvReviewerInitial);
            tvName    = v.findViewById(R.id.tvReviewerName);
            tvDate    = v.findViewById(R.id.tvReviewDate);
            tvRating  = v.findViewById(R.id.tvReviewRating);
            tvComment = v.findViewById(R.id.tvReviewComment);
        }
    }
}
