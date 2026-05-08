package com.example.pickleball.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.model.Booking;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    public interface OnBookingClickListener {
        void onBookingClick(Booking booking);
    }

    private final List<Booking> bookingList;
    private OnBookingClickListener clickListener;

    public BookingAdapter(List<Booking> bookingList) {
        this.bookingList = bookingList;
    }

    public BookingAdapter(List<Booking> bookingList, OnBookingClickListener listener) {
        this.bookingList = bookingList;
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking b = bookingList.get(position);
        Context ctx = holder.itemView.getContext();

        // Tên sân
        String courtName = b.getCourtName();
        holder.tvBookingId.setText(courtName != null && !courtName.isEmpty()
                ? courtName : "Sân #" + (b.getCourtId() != null ? b.getCourtId().substring(0, Math.min(6, b.getCourtId().length())) : "?"));

        // Ngày + giờ
        String timeInfo = b.getDate() != null ? b.getDate() : "";
        if (b.getStartTime() != null && b.getEndTime() != null) {
            timeInfo += "  " + b.getStartTime() + " – " + b.getEndTime();
        }
        holder.tvDate.setText(timeInfo.isEmpty() ? "Chưa có thông tin" : timeInfo);

        // Giá
        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
        holder.tvPrice.setText(fmt.format(b.getTotalPrice()) + "đ");

        // Status với màu sắc
        String status = b.getStatus();
        if (status == null) status = "pending";
        switch (status) {
            case "confirmed":
                holder.tvStatus.setText("✅ Đã xác nhận");
                holder.tvStatus.setTextColor(ctx.getColor(R.color.green_dark));
                break;
            case "rejected":
                holder.tvStatus.setText("❌ Đã từ chối");
                holder.tvStatus.setTextColor(ctx.getColor(R.color.error_red));
                break;
            case "cancelled":
                holder.tvStatus.setText("🚫 Đã hủy");
                holder.tvStatus.setTextColor(ctx.getColor(R.color.text_tertiary));
                break;
            default: // pending
                holder.tvStatus.setText("⏳ Chờ xác nhận");
                holder.tvStatus.setTextColor(ctx.getColor(R.color.warning_yellow));
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onBookingClick(b);
        });
    }

    @Override
    public int getItemCount() {
        return bookingList == null ? 0 : bookingList.size();
    }

    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvBookingId, tvDate, tvStatus, tvPrice;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBookingId = itemView.findViewById(R.id.tvBookingId);
            tvDate      = itemView.findViewById(R.id.tvDate);
            tvStatus    = itemView.findViewById(R.id.tvStatus);
            tvPrice     = itemView.findViewById(R.id.tvPrice);
        }
    }
}
