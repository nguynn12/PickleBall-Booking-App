package com.example.pickleball.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.model.SelectedSlot;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class SelectedSlotAdapter extends RecyclerView.Adapter<SelectedSlotAdapter.ViewHolder> {

    private final List<SelectedSlot> list;

    public SelectedSlotAdapter(List<SelectedSlot> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_slot, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        SelectedSlot s = list.get(position);
        // "Pickleball 1: 17h00 - 17h30"
        String timeLabel = s.getStartTime().replace(":00", "h").replace(":30", "h30")
                + " - " + s.getEndTime().replace(":00", "h").replace(":30", "h30");
        h.tvSubCourt.setText(s.getSubCourtName() + ": " + timeLabel);

        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
        h.tvPrice.setText(fmt.format(s.getPrice()) + " đ");
    }

    @Override
    public int getItemCount() { return list == null ? 0 : list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubCourt, tvPrice;
        ViewHolder(@NonNull View v) {
            super(v);
            tvSubCourt = v.findViewById(R.id.tvSlotSubCourt);
            tvPrice    = v.findViewById(R.id.tvSlotPrice);
        }
    }
}
