package com.example.pickleball.fragment.court;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.adapter.ReviewAdapter;
import com.example.pickleball.model.Court;
import com.example.pickleball.model.Review;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TabReviewsFragment extends Fragment {

    private static final String ARG_COURT = "court";
    private final List<Review> reviewList = new ArrayList<>();
    private ReviewAdapter adapter;
    private Court court;

    public static TabReviewsFragment newInstance(Court court) {
        TabReviewsFragment f = new TabReviewsFragment();
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
        return inflater.inflate(R.layout.fragment_tab_reviews, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        court = getArguments() != null
                ? (Court) getArguments().getSerializable(ARG_COURT) : null;
        if (court == null) return;

        RecyclerView rv = view.findViewById(R.id.rvReviews);
        TextView tvNoReviews = view.findViewById(R.id.tvNoReviews);
        TextView tvAvg = view.findViewById(R.id.tvAvgRating);
        TextView tvCount = view.findViewById(R.id.tvReviewCount);

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setNestedScrollingEnabled(false);
        adapter = new ReviewAdapter(reviewList);
        rv.setAdapter(adapter);

        view.findViewById(R.id.btnWriteReview).setOnClickListener(v -> showWriteReviewDialog());

        // Load reviews
        FirebaseFirestore.getInstance()
                .collection("Reviews")
                .whereEqualTo("courtId", court.getCourtId())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, err) -> {
                    if (snap == null) return;
                    reviewList.clear();
                    float total = 0;
                    for (var doc : snap.getDocuments()) {
                        Review r = doc.toObject(Review.class);
                        if (r != null) {
                            if (r.getReviewId() == null) r.setReviewId(doc.getId());
                            reviewList.add(r);
                            total += r.getRating();
                        }
                    }
                    adapter.notifyDataSetChanged();
                    int count = reviewList.size();
                    tvNoReviews.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
                    rv.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
                    if (count > 0) {
                        float avg = total / count;
                        tvAvg.setText(String.format(Locale.getDefault(), "%.1f", avg));
                        tvCount.setText(count + " đánh giá");
                    } else {
                        tvAvg.setText("–");
                        tvCount.setText("Chưa có đánh giá");
                    }
                });
    }

    private void showWriteReviewDialog() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để đánh giá!", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(android.R.layout.simple_list_item_2, null);

        // Build custom dialog
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText("Đánh giá sân");
        tvTitle.setTextSize(16f);
        tvTitle.setTextColor(requireContext().getColor(R.color.text_primary));
        layout.addView(tvTitle);

        RatingBar ratingBar = new RatingBar(requireContext());
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1f);
        ratingBar.setRating(5f);
        LinearLayout.LayoutParams rbLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rbLp.topMargin = 16;
        ratingBar.setLayoutParams(rbLp);
        layout.addView(ratingBar);

        EditText edtComment = new EditText(requireContext());
        edtComment.setHint("Nhận xét của bạn...");
        edtComment.setMinLines(3);
        LinearLayout.LayoutParams edtLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        edtLp.topMargin = 16;
        edtComment.setLayoutParams(edtLp);
        layout.addView(edtComment);

        new AlertDialog.Builder(requireContext())
                .setView(layout)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String comment = edtComment.getText().toString().trim();
                    float rating = ratingBar.getRating();
                    submitReview(rating, comment);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void submitReview(float rating, String comment) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("Users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("fullName");
                    if (name == null) name = "Người dùng";
                    Review review = new Review(court.getCourtId(), uid, name, rating, comment);
                    FirebaseFirestore.getInstance().collection("Reviews")
                            .add(review)
                            .addOnSuccessListener(ref -> {
                                ref.update("reviewId", ref.getId());
                                Toast.makeText(requireContext(), "Cảm ơn bạn đã đánh giá!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                });
    }
}
