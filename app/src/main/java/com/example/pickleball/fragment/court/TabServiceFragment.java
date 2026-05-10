package com.example.pickleball.fragment.court;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.pickleball.R;
import com.example.pickleball.model.Court;
import com.example.pickleball.model.CourtService;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TabServiceFragment extends Fragment {

    private static final String ARG_COURT = "court";

    public static TabServiceFragment newInstance(Court court) {
        TabServiceFragment f = new TabServiceFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_COURT, court);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tab_service, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Court court = getArguments() != null
                ? (Court) getArguments().getSerializable(ARG_COURT) : null;
        if (court == null) return;

        TextView tvTitle = view.findViewById(R.id.tvPriceTableTitle);
        tvTitle.setText(court.getType() != null ? court.getType() : "Pickleball");

        LinearLayout layoutPriceRows   = view.findViewById(R.id.layoutPriceRows);
        LinearLayout layoutServiceRows = view.findViewById(R.id.layoutServiceRows);

        // Load services từ Firestore
        if (court.getCourtId() != null) {
            FirebaseFirestore.getInstance()
                    .collection("Courts")
                    .document(court.getCourtId())
                    .collection("Services")
                    .get()
                    .addOnSuccessListener(snap -> {
                        List<CourtService> priceList   = new ArrayList<>();
                        List<CourtService> serviceList = new ArrayList<>();

                        for (var doc : snap.getDocuments()) {
                            CourtService s = doc.toObject(CourtService.class);
                            if (s == null) continue;
                            if ("price_table".equals(s.getType())) priceList.add(s);
                            else serviceList.add(s);
                        }

                        // Nếu chưa có data, hiển thị mặc định từ pricePerHour
                        if (priceList.isEmpty()) {
                            addDefaultPriceRow(layoutPriceRows, court);
                        } else {
                            for (CourtService s : priceList) {
                                addPriceRow(layoutPriceRows, s.getDayRange(),
                                        s.getTimeRange(), s.getPrice());
                            }
                        }

                        if (serviceList.isEmpty()) {
                            addServiceRow(layoutServiceRows, "Chưa có dịch vụ", 0, "");
                        } else {
                            for (int i = 0; i < serviceList.size(); i++) {
                                CourtService s = serviceList.get(i);
                                addServiceRow(layoutServiceRows, s.getName(),
                                        s.getPrice(), s.getUnit());
                                if (i < serviceList.size() - 1) addDivider(layoutServiceRows);
                            }
                        }
                    });
        } else {
            addDefaultPriceRow(layoutPriceRows, court);
            addServiceRow(layoutServiceRows, "Chưa có dịch vụ", 0, "");
        }
    }

    private void addDefaultPriceRow(LinearLayout parent, Court court) {
        double price = court.getPricePerHour();
        addPriceRow(parent, "T2 - T6", court.getOpenHours(), price);
        addDivider(parent);
        addPriceRow(parent, "T7 - CN", court.getOpenHours(), price > 0 ? price * 1.3 : 0);
    }

    private void addPriceRow(LinearLayout parent, String day, String time, double price) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        int dp8 = dp(8);
        int dp12 = dp(12);

        TextView tvDay = makeCell(day, 1f, true);
        tvDay.setPadding(dp8, dp12, dp8, dp12);

        View div1 = new View(requireContext());
        div1.setLayoutParams(new LinearLayout.LayoutParams(1, LinearLayout.LayoutParams.MATCH_PARENT));
        div1.setBackgroundColor(getResources().getColor(R.color.divider, null));

        TextView tvTime = makeCell(time, 1.5f, false);
        tvTime.setPadding(dp8, dp12, dp8, dp12);

        View div2 = new View(requireContext());
        div2.setLayoutParams(new LinearLayout.LayoutParams(1, LinearLayout.LayoutParams.MATCH_PARENT));
        div2.setBackgroundColor(getResources().getColor(R.color.divider, null));

        String priceStr = price > 0
                ? NumberFormat.getInstance(new Locale("vi", "VN")).format(price) + " đ"
                : "Liên hệ";
        TextView tvPrice = makeCell(priceStr, 1.5f, false);
        tvPrice.setPadding(dp8, dp12, dp8, dp12);
        tvPrice.setTextColor(getResources().getColor(R.color.text_primary, null));

        row.addView(tvDay);
        row.addView(div1);
        row.addView(tvTime);
        row.addView(div2);
        row.addView(tvPrice);
        parent.addView(row);
    }

    private void addServiceRow(LinearLayout parent, String name, double price, String unit) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int dp14 = dp(14); int dp12 = dp(12);
        row.setPadding(dp14, dp12, dp14, dp12);

        TextView tvName = new TextView(requireContext());
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvName.setText(name);
        tvName.setTextSize(14f);
        tvName.setTextColor(getResources().getColor(R.color.text_primary, null));

        TextView tvPrice = new TextView(requireContext());
        tvPrice.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        if (price > 0) {
            String fmt = NumberFormat.getInstance(new Locale("vi", "VN")).format(price)
                    + " đ" + (unit != null && !unit.isEmpty() ? " / " + unit : "");
            tvPrice.setText(fmt);
        } else {
            tvPrice.setText("");
        }
        tvPrice.setTextSize(14f);
        tvPrice.setTextColor(getResources().getColor(R.color.text_secondary, null));

        row.addView(tvName);
        row.addView(tvPrice);
        parent.addView(row);
    }

    private void addDivider(LinearLayout parent) {
        View div = new View(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.setMarginStart(dp(14));
        div.setLayoutParams(lp);
        div.setBackgroundColor(getResources().getColor(R.color.divider, null));
        parent.addView(div);
    }

    private TextView makeCell(String text, float weight, boolean center) {
        TextView tv = new TextView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, weight);
        tv.setLayoutParams(lp);
        tv.setText(text != null ? text : "");
        tv.setTextSize(13f);
        tv.setTextColor(getResources().getColor(R.color.text_secondary, null));
        tv.setGravity(center ? Gravity.CENTER : Gravity.CENTER);
        return tv;
    }

    private int dp(int value) {
        return (int) (value * requireContext().getResources().getDisplayMetrics().density);
    }
}
