package com.example.pickleball.activity.booking;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.pickleball.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.example.pickleball.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PaymentActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID   = "bookingId";
    public static final String EXTRA_DEPOSIT       = "depositAmount";
    public static final String EXTRA_TOTAL         = "totalAmount";
    public static final String EXTRA_COURT_NAME    = "courtName";
    public static final String EXTRA_DATE          = "bookingDate";
    public static final String EXTRA_CUSTOMER_NAME = "customerName";

    private String bookingId, courtName, bookingDate, customerName;
    private double depositAmount, totalAmount;

    private ImageView imgQrCode;
    private TextView tvDepositAmount, tvRemainingAmount, tvCountdown, tvTransferContent;
    private LinearLayout layoutQrLoading, layoutQrContent;
    private MaterialButton btnCancelPayment;

    private CountDownTimer countDownTimer;
    private ListenerRegistration bookingListener;
    private boolean finished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        bookingId     = getIntent().getStringExtra(EXTRA_BOOKING_ID);
        depositAmount = getIntent().getDoubleExtra(EXTRA_DEPOSIT, 0);
        totalAmount   = getIntent().getDoubleExtra(EXTRA_TOTAL, 0);
        courtName     = getIntent().getStringExtra(EXTRA_COURT_NAME);
        bookingDate   = getIntent().getStringExtra(EXTRA_DATE);
        customerName  = getIntent().getStringExtra(EXTRA_CUSTOMER_NAME);

        if (bookingId == null) { finish(); return; }

        bindViews();
        bindStaticInfo();
        fetchPaymentQR();
        listenForPaymentConfirmation();
        startCountdown(Constants.PAYMENT_TIMEOUT_MS);
    }

    private void bindViews() {
        imgQrCode         = findViewById(R.id.imgQrCode);
        tvDepositAmount   = findViewById(R.id.tvDepositAmount);
        tvRemainingAmount = findViewById(R.id.tvRemainingAmount);
        tvCountdown       = findViewById(R.id.tvCountdown);
        tvTransferContent = findViewById(R.id.tvTransferContent);
        layoutQrLoading   = findViewById(R.id.layoutQrLoading);
        layoutQrContent   = findViewById(R.id.layoutQrContent);
        btnCancelPayment  = findViewById(R.id.btnCancelPayment);
        btnCancelPayment.setOnClickListener(v -> confirmCancel());
    }

    private void bindStaticInfo() {
        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
        tvDepositAmount.setText(fmt.format((long) depositAmount) + "đ");
        tvRemainingAmount.setText(fmt.format((long) (totalAmount - depositAmount)) + "đ");
        tvTransferContent.setText("Coc san " + bookingId.substring(Math.max(0, bookingId.length() - 6)));
    }

    private void fetchPaymentQR() {
        layoutQrLoading.setVisibility(View.VISIBLE);
        layoutQrContent.setVisibility(View.GONE);

        Map<String, Object> data = new HashMap<>();
        data.put("bookingId",    bookingId);
        data.put("amount",       totalAmount);
        data.put("customerName", customerName);
        data.put("courtName",    courtName);
        data.put("bookingDate",  bookingDate);

        String json = new Gson().toJson(data);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(Constants.CLOUD_FN_CREATE_PAYMENT)
                .post(body)
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    try {
                        JSONObject obj = new JSONObject(responseBody);
                        if (obj.optBoolean("success")) {
                            if (obj.optBoolean("alreadyPaid")) {
                                openSuccessScreen();
                                return;
                            }
                            String qrCode = obj.optString("qrCode");
                            displayQR(qrCode);
                        } else {
                            showQRError();
                        }
                    } catch (Exception e) {
                        showQRError();
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(PaymentActivity.this::showQRError);
            }
        });
    }

    private void displayQR(String qrCodeData) {
        layoutQrLoading.setVisibility(View.GONE);
        layoutQrContent.setVisibility(View.VISIBLE);
        try {
            int size = 600;
            BitMatrix matrix = new MultiFormatWriter().encode(
                    qrCodeData, BarcodeFormat.QR_CODE, size, size);
            Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            for (int x = 0; x < size; x++)
                for (int y = 0; y < size; y++)
                    bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            imgQrCode.setImageBitmap(bmp);
        } catch (Exception e) {
            showQRError();
        }
    }

    private void showQRError() {
        layoutQrLoading.setVisibility(View.GONE);
        layoutQrContent.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Không tạo được QR. Vui lòng chuyển khoản thủ công.", Toast.LENGTH_LONG).show();
    }

    private void listenForPaymentConfirmation() {
        bookingListener = FirebaseFirestore.getInstance()
                .collection("Bookings")
                .document(bookingId)
                .addSnapshotListener((snap, err) -> {
                    if (snap == null || finished) return;
                    String status        = snap.getString("status");
                    String paymentStatus = snap.getString("paymentStatus");

                    if (Constants.PAYMENT_STATUS_PAID.equals(paymentStatus)) {
                        openSuccessScreen();
                    } else if (status != null && status.startsWith("cancelled")) {
                        runOnUiThread(() ->
                                Toast.makeText(this, "Đặt sân đã bị hủy.", Toast.LENGTH_LONG).show());
                        finishCleanup();
                        finish();
                    }
                });
    }

    private void startCountdown(long millis) {
        countDownTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long msLeft) {
                long min = msLeft / 60000;
                long sec = (msLeft % 60000) / 1000;
                tvCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));
            }

            @Override
            public void onFinish() {
                tvCountdown.setText("00:00");
                Toast.makeText(PaymentActivity.this, "Đã hết thời gian thanh toán!", Toast.LENGTH_LONG).show();
            }
        }.start();
    }

    private void openSuccessScreen() {
        if (finished) return;
        finished = true;
        finishCleanup();
        Intent intent = new Intent(this, BookingSuccessActivity.class);
        intent.putExtra(BookingSuccessActivity.EXTRA_BOOKING_ID,  bookingId);
        intent.putExtra(BookingSuccessActivity.EXTRA_COURT_NAME,  courtName);
        intent.putExtra(BookingSuccessActivity.EXTRA_DATE,        bookingDate);
        intent.putExtra(BookingSuccessActivity.EXTRA_REMAINING,   totalAmount - depositAmount);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void confirmCancel() {
        new AlertDialog.Builder(this)
                .setTitle("Hủy thanh toán")
                .setMessage("Bạn có chắc muốn hủy? Lịch đặt sân sẽ bị xóa.")
                .setPositiveButton("Hủy đặt sân", (d, w) -> cancelBooking())
                .setNegativeButton("Tiếp tục thanh toán", null)
                .show();
    }

    private void cancelBooking() {
        FirebaseFirestore.getInstance()
                .collection("Bookings").document(bookingId)
                .update("status", Constants.BOOKING_STATUS_CANCELLED_BY_USER,
                        "cancelledBy", "user",
                        "cancelledAt", System.currentTimeMillis());
        finishCleanup();
        finish();
    }

    private void finishCleanup() {
        if (bookingListener != null) { bookingListener.remove(); bookingListener = null; }
        if (countDownTimer  != null) { countDownTimer.cancel();  countDownTimer  = null; }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finishCleanup();
    }
}
