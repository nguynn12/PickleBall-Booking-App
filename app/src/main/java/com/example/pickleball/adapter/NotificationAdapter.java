package com.example.pickleball.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.model.Notification;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnClickListener {
        void onClick(Notification notif);
    }

    private final List<Notification> list;
    private final OnClickListener listener;

    public NotificationAdapter(List<Notification> list, OnClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Notification n = list.get(position);

        h.tvTitle.setText(n.getTitle() != null ? n.getTitle() : "");
        h.tvMessage.setText(n.getMessage() != null ? n.getMessage() : "");

        // Dot chưa đọc
        h.dotUnread.setVisibility(n.isRead() ? View.INVISIBLE : View.VISIBLE);

        // Thời gian
        if (n.getCreatedAt() > 0) {
            long diff = System.currentTimeMillis() - n.getCreatedAt();
            String timeStr;
            if (diff < 60_000) {
                timeStr = "Vừa xong";
            } else if (diff < 3_600_000) {
                timeStr = (diff / 60_000) + " phút trước";
            } else if (diff < 86_400_000) {
                timeStr = (diff / 3_600_000) + " giờ trước";
            } else {
                timeStr = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"))
                        .format(new Date(n.getCreatedAt()));
            }
            h.tvTime.setText(timeStr);
        }

        // Màu nền khác nhau nếu chưa đọc
        h.itemView.setAlpha(n.isRead() ? 0.75f : 1f);
        h.itemView.setOnClickListener(v -> listener.onClick(n));
    }

    @Override
    public int getItemCount() { return list == null ? 0 : list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime;
        View dotUnread;

        ViewHolder(@NonNull View v) {
            super(v);
            tvTitle   = v.findViewById(R.id.tvNotifTitle);
            tvMessage = v.findViewById(R.id.tvNotifMessage);
            tvTime    = v.findViewById(R.id.tvNotifTime);
            dotUnread = v.findViewById(R.id.dotUnread);
        }
    }
}
