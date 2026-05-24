# PickleBook — Tích hợp Đặt Cọc & Thanh Toán PayOS
> **Dành cho:** Claude Sonnet 4.6 (AI Coder)  
> **Phạm vi:** Hệ thống đặt cọc 30% + QR VietQR qua PayOS  
> **Stack:** Android Java · Firebase · PayOS SDK/API  
> **Đọc trước:** `PICKLEBOOK_REFACTOR_GUIDE.md` (đặc biệt phần bỏ bước xác nhận chủ sân)

---

## Mục lục
1. [Tổng quan luồng mới](#1-tổng-quan-luồng-mới)
2. [Chính sách đặt cọc & hoàn tiền](#2-chính-sách-đặt-cọc--hoàn-tiền)
3. [Tích hợp PayOS](#3-tích-hợp-payos)
4. [Thay đổi Database](#4-thay-đổi-database)
5. [Thay đổi Booking Flow](#5-thay-đổi-booking-flow)
6. [Màn hình thanh toán](#6-màn-hình-thanh-toán)
7. [Xử lý Webhook](#7-xử-lý-webhook)
8. [Màn hình quản lý chủ sân](#8-màn-hình-quản-lý-chủ-sân)
9. [Màn hình Admin](#9-màn-hình-admin)
10. [Edge Cases cần xử lý](#10-edge-cases-cần-xử-lý)
11. [Thứ tự thực hiện](#11-thứ-tự-thực-hiện)

---

## 1. Tổng quan luồng mới

### Luồng đặt sân (thay thế luồng cũ hoàn toàn)

```
Khách chọn slot
        ↓
BookingConfirmActivity
        ↓
Tạo Booking (status = "awaiting_payment")
        ↓
PaymentActivity — hiển thị QR VietQR
  - Số tiền cọc = totalPrice * 30%
  - Mã đơn hàng = bookingId
  - Countdown 15 phút (hết giờ → hủy booking tự động)
        ↓
  [Khách quét QR chuyển khoản]
        ↓
PayOS Webhook → Firebase Cloud Function
  - Verify thanh toán thành công
  - Update Booking status = "confirmed"
  - Ô slot chuyển đỏ (realtime)
  - Gửi notification cho khách + chủ sân
        ↓
Khách nhận màn hình "Đặt sân thành công"
```

### So sánh với luồng cũ

| | Luồng cũ | Luồng mới |
|---|---|---|
| Sau khi khách đặt | status = "pending", chờ chủ sân | status = "awaiting_payment" |
| Chủ sân phải làm | Xác nhận từng đơn | Không cần làm gì |
| Slot chuyển đỏ khi nào | Sau khi chủ sân xác nhận | Sau khi thanh toán cọc xong |
| Khách ghost | Không có hậu quả | Mất 30% tiền cọc |
| Hủy đúng hạn | Không có logic | Hoàn 100% cọc |

---

## 2. Chính sách đặt cọc & hoàn tiền

### Quy tắc cố định (hardcode vào Constants.java)

```java
// utils/Constants.java — thêm các hằng số này

// Đặt cọc
public static final double DEPOSIT_RATE = 0.30;          // 30% tổng tiền
public static final long PAYMENT_TIMEOUT_MS = 15 * 60 * 1000L; // 15 phút để thanh toán QR

// Chính sách hủy
public static final long FREE_CANCEL_WINDOW_MS = 2 * 60 * 60 * 1000L; // 2 tiếng trước giờ chơi

// Trạng thái booking mới
public static final String BOOKING_STATUS_AWAITING_PAYMENT = "awaiting_payment";
public static final String BOOKING_STATUS_CONFIRMED         = "confirmed";
public static final String BOOKING_STATUS_CANCELLED_BY_USER = "cancelled_by_user";
public static final String BOOKING_STATUS_CANCELLED_NO_SHOW = "cancelled_no_show";
public static final String BOOKING_STATUS_CANCELLED_BY_OWNER= "cancelled_by_owner";
public static final String BOOKING_STATUS_COMPLETED         = "completed";

// Trạng thái hoàn tiền
public static final String REFUND_STATUS_PENDING  = "refund_pending";
public static final String REFUND_STATUS_DONE     = "refund_done";
public static final String REFUND_STATUS_REJECTED = "refund_rejected";
public static final String REFUND_STATUS_NA       = "not_applicable"; // không áp dụng (mất cọc)
```

### Bảng logic hoàn tiền

```
Tình huống                          | Hoàn tiền cọc?
------------------------------------|---------------------------
Hủy trước giờ chơi >= 2 tiếng      | Hoàn 100% cọc
Hủy trước giờ chơi < 2 tiếng       | Mất 100% cọc
Không hủy, không tới (no-show)      | Mất 100% cọc
Chủ sân hủy (bất kỳ lý do)         | Hoàn 100% cọc
Thanh toán timeout (15 phút)        | Không thu, booking tự hủy
Lỗi hệ thống                       | Hoàn 100% cọc + ghi log
```

**Lưu ý quan trọng:** Hoàn tiền trong giai đoạn đầu vẫn là **thủ công** — Admin nhận yêu cầu hoàn tiền, chuyển khoản tay cho khách. PayOS có API hoàn tiền tự động nhưng cần cấu hình thêm, để Sprint sau.

---

## 3. Tích hợp PayOS

### 3.1 PayOS là gì

PayOS là payment gateway Việt Nam, hỗ trợ VietQR, không cần đăng ký doanh nghiệp để sandbox test. Chi phí production: 1.000đ/giao dịch thành công (không % như VNPay).

Website: https://payos.vn  
Docs: https://docs.payos.vn

### 3.2 Đăng ký tài khoản

1. Vào https://my.payos.vn/register
2. Đăng ký tài khoản cá nhân (cho giai đoạn dev/test)
3. Vào Settings → API Keys → lấy:
   - `Client ID`
   - `API Key`
   - `Checksum Key`
4. Cấu hình Webhook URL (xem mục 7)

### 3.3 Kiến trúc tích hợp

```
Android App
    ↓ (POST tạo payment link)
Firebase Cloud Function (Node.js) ← Backend bắt buộc, KHÔNG gọi PayOS trực tiếp từ app
    ↓ (gọi PayOS API)
PayOS Server
    ↓ (trả về QR data + payment link)
Firebase Cloud Function
    ↓ (trả về cho app)
Android App hiển thị QR

--- Sau khi khách thanh toán ---

PayOS Server
    ↓ (gọi Webhook)
Firebase Cloud Function
    ↓ (verify + update Firestore)
Firestore realtime → Android App tự cập nhật
```

**Tại sao không gọi PayOS trực tiếp từ app?**  
Vì `API Key` và `Checksum Key` phải được giữ bí mật ở server. Nếu nhúng vào APK, ai cũng có thể decompile và lấy key.

### 3.4 Firebase Cloud Functions cần tạo

```
functions/
├── index.js
├── createPaymentLink.js    ← Tạo QR PayOS
├── payosWebhook.js         ← Nhận callback từ PayOS
└── cancelExpiredBookings.js ← Cron job hủy booking timeout
```

#### `createPaymentLink.js`

```javascript
const axios = require('axios');
const crypto = require('crypto');

const PAYOS_CLIENT_ID  = process.env.PAYOS_CLIENT_ID;
const PAYOS_API_KEY    = process.env.PAYOS_API_KEY;
const PAYOS_CHECKSUM   = process.env.PAYOS_CHECKSUM_KEY;

exports.createPaymentLink = async (req, res) => {
    // Nhận từ Android app:
    const { bookingId, amount, customerName, courtName, bookingDate } = req.body;

    // Tính số tiền cọc
    const depositAmount = Math.round(amount * 0.30);
    
    // Tạo orderCode = số nguyên dương, unique
    // Dùng timestamp để đảm bảo unique
    const orderCode = parseInt(Date.now().toString().slice(-9));

    // Tạo description (tối đa 25 ký tự cho VietQR)
    const description = `Coc san ${bookingId.slice(-6)}`;

    // Tạo chữ ký HMAC
    const dataStr = `amount=${depositAmount}&cancelUrl=https://yourapp.com/cancel&description=${description}&orderCode=${orderCode}&returnUrl=https://yourapp.com/success`;
    const signature = crypto
        .createHmac('sha256', PAYOS_CHECKSUM)
        .update(dataStr)
        .digest('hex');

    const payload = {
        orderCode,
        amount: depositAmount,
        description,
        cancelUrl:  'https://yourapp.com/cancel',  // deep link hoặc URL placeholder
        returnUrl:  'https://yourapp.com/success',
        signature,
        items: [{
            name: `Đặt sân ${courtName}`,
            quantity: 1,
            price: depositAmount
        }],
        buyerName: customerName,
        expiredAt: Math.floor(Date.now() / 1000) + 900 // 15 phút
    };

    try {
        const response = await axios.post(
            'https://api-merchant.payos.vn/v2/payment-requests',
            payload,
            {
                headers: {
                    'x-client-id': PAYOS_CLIENT_ID,
                    'x-api-key':   PAYOS_API_KEY,
                    'Content-Type': 'application/json'
                }
            }
        );

        const { checkoutUrl, qrCode, paymentLinkId } = response.data.data;

        // Lưu orderCode + paymentLinkId vào Firestore Booking
        const admin = require('firebase-admin');
        await admin.firestore()
            .collection('Bookings')
            .doc(bookingId)
            .update({
                payosOrderCode:     orderCode,
                payosPaymentLinkId: paymentLinkId,
                depositAmount:      depositAmount,
                paymentStatus:      'pending',
                paymentExpiredAt:   Date.now() + 900_000
            });

        res.json({ success: true, qrCode, checkoutUrl, orderCode, depositAmount });

    } catch (error) {
        console.error('PayOS error:', error.response?.data || error.message);
        res.status(500).json({ success: false, error: 'Lỗi tạo QR thanh toán' });
    }
};
```

#### `payosWebhook.js`

```javascript
const crypto = require('crypto');
const admin  = require('firebase-admin');

exports.payosWebhook = async (req, res) => {
    const PAYOS_CHECKSUM = process.env.PAYOS_CHECKSUM_KEY;

    // 1. Verify chữ ký từ PayOS
    const { code, desc, data, signature } = req.body;
    
    const dataStr = Object.keys(data)
        .sort()
        .map(k => `${k}=${data[k]}`)
        .join('&');
    
    const expectedSig = crypto
        .createHmac('sha256', PAYOS_CHECKSUM)
        .update(dataStr)
        .digest('hex');

    if (expectedSig !== signature) {
        return res.status(400).json({ error: 'Invalid signature' });
    }

    // 2. Xử lý theo code
    if (code === '00') {
        // Thanh toán thành công
        const orderCode = data.orderCode;

        // Tìm booking theo orderCode
        const snap = await admin.firestore()
            .collection('Bookings')
            .where('payosOrderCode', '==', orderCode)
            .limit(1)
            .get();

        if (!snap.empty) {
            const bookingDoc = snap.docs[0];
            const booking    = bookingDoc.data();

            // Cập nhật Booking
            await bookingDoc.ref.update({
                status:        'confirmed',
                paymentStatus: 'paid',
                paidAt:        admin.firestore.FieldValue.serverTimestamp(),
                payosTransactionId: data.id
            });

            // Gửi notification cho khách
            await sendNotification(booking.userId,
                '✅ Đặt sân thành công!',
                `Bạn đã cọc thành công cho sân ${booking.courtName} vào ngày ${booking.date}.`
            );

            // Gửi notification cho chủ sân
            await sendNotification(booking.ownerId,
                '🏓 Có lịch đặt mới!',
                `Khách đã đặt sân ${booking.courtName} ngày ${booking.date} lúc ${booking.startTime}.`
            );
        }
    }

    res.json({ success: true });
};

async function sendNotification(userId, title, message) {
    await admin.firestore().collection('Notifications').add({
        userId,
        title,
        message,
        isRead:    false,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
}
```

#### `cancelExpiredBookings.js` (Cron job chạy mỗi 5 phút)

```javascript
// Tự động hủy booking chưa thanh toán sau 15 phút
exports.cancelExpiredBookings = async (context) => {
    const now   = Date.now();
    const admin = require('firebase-admin');

    const snap = await admin.firestore()
        .collection('Bookings')
        .where('status', '==', 'awaiting_payment')
        .where('paymentExpiredAt', '<', now)
        .get();

    const batch = admin.firestore().batch();
    snap.docs.forEach(doc => {
        batch.update(doc.ref, {
            status:        'cancelled_timeout',
            paymentStatus: 'expired'
        });
    });

    await batch.commit();
    console.log(`Cancelled ${snap.size} expired bookings`);
};
```

```javascript
// index.js — export tất cả functions
const functions = require('firebase-functions');
const admin     = require('firebase-admin');
admin.initializeApp();

const { createPaymentLink }    = require('./createPaymentLink');
const { payosWebhook }         = require('./payosWebhook');
const { cancelExpiredBookings} = require('./cancelExpiredBookings');

// HTTP functions
exports.createPaymentLink = functions.https.onRequest(createPaymentLink);
exports.payosWebhook      = functions.https.onRequest(payosWebhook);

// Scheduled function (cron)
exports.cancelExpiredBookings = functions.pubsub
    .schedule('every 5 minutes')
    .onRun(cancelExpiredBookings);
```

### 3.5 Cấu hình environment variables

```bash
# Chạy lệnh này để set config cho Firebase Functions
firebase functions:config:set \
  payos.client_id="YOUR_CLIENT_ID" \
  payos.api_key="YOUR_API_KEY" \
  payos.checksum_key="YOUR_CHECKSUM_KEY"
```

---

## 4. Thay đổi Database

### 4.1 Booking model — thêm fields mới

```java
// model/Booking.java — thêm các fields sau

// --- Thanh toán ---
private double depositAmount;       // Số tiền cọc (totalPrice * 30%)
private double remainingAmount;     // Số tiền còn lại (totalPrice * 70%)
private String paymentStatus;       // "pending" | "paid" | "refunded" | "expired"
private long   paidAt;              // timestamp khi thanh toán xong
private long   paymentExpiredAt;    // timestamp hết hạn QR (createdAt + 15 phút)

// --- PayOS ---
private long   payosOrderCode;      // mã đơn hàng PayOS (số nguyên)
private String payosPaymentLinkId;  // ID link thanh toán PayOS
private String payosTransactionId;  // ID giao dịch sau khi thanh toán

// --- Hủy & hoàn tiền ---
private String cancelReason;        // lý do hủy
private String cancelledBy;         // "user" | "owner" | "system"
private long   cancelledAt;         // timestamp hủy
private String refundStatus;        // "not_applicable" | "refund_pending" | "refund_done"
private long   refundAmount;        // số tiền được hoàn

// Getters & Setters cho tất cả fields trên...
```

### 4.2 Cập nhật status flow

```
Status cũ (XÓA):        Status mới:
─────────────────        ──────────────────────────
"pending"           →    "awaiting_payment"
"confirmed"         →    "confirmed"          (giữ nguyên)
"rejected"          →    XÓA (chủ sân không còn reject)
"cancelled"         →    "cancelled_by_user"
                         "cancelled_by_owner"
                         "cancelled_no_show"
                         "cancelled_timeout"  (hết 15 phút không trả)
                    →    "completed"          (thêm mới, sau buổi chơi)
```

### 4.3 Firestore indexes cần tạo

Vào Firebase Console → Firestore → Indexes → thêm:

```
Collection: Bookings
Fields:     status ASC, paymentExpiredAt ASC
            (dùng cho cancelExpiredBookings cron job)

Collection: Bookings  
Fields:     userId ASC, createdAt DESC
            (dùng cho MyBookingsFragment)

Collection: Bookings
Fields:     ownerId ASC, date ASC, startTime ASC
            (dùng cho chủ sân xem lịch)
```

---

## 5. Thay đổi Booking Flow

### 5.1 BookingConfirmActivity — thay đổi confirmBooking()

```java
// activity/booking/BookingConfirmActivity.java

private void confirmBooking() {
    // ... validate name, phone, uid như cũ ...

    btnConfirm.setEnabled(false);
    btnConfirm.setText("Đang xử lý...");

    // Tính tiền
    double totalMoney   = 0;
    for (SelectedSlot s : selectedSlots) totalMoney += s.getPrice();
    double depositMoney = Math.round(totalMoney * Constants.DEPOSIT_RATE);

    // Tạo bookings
    List<Booking> bookings = mergeSlots(uid, note);
    FirebaseFirestore db   = FirebaseFirestore.getInstance();

    // Lưu booking với status = "awaiting_payment"
    for (Booking b : bookings) {
        b.setStatus(Constants.BOOKING_STATUS_AWAITING_PAYMENT);  // ← thay vì "pending"
        b.setDepositAmount(depositMoney / bookings.size());
        b.setRemainingAmount((totalMoney - depositMoney) / bookings.size());
        b.setPaymentStatus("pending");
        b.setPaymentExpiredAt(System.currentTimeMillis() + Constants.PAYMENT_TIMEOUT_MS);

        db.collection("Bookings").add(b)
            .addOnSuccessListener(ref -> {
                ref.update("bookingId", ref.getId());

                // Sau khi tạo booking → mở màn hình thanh toán
                Intent intent = new Intent(this, PaymentActivity.class);
                intent.putExtra("bookingId",    ref.getId());
                intent.putExtra("depositAmount", b.getDepositAmount());
                intent.putExtra("totalAmount",   b.getTotalPrice());
                intent.putExtra("courtName",     court.getCourtName());
                intent.putExtra("bookingDate",   selectedDate);
                intent.putExtra("customerName",  name);
                startActivity(intent);
                finish();
            })
            .addOnFailureListener(e -> {
                btnConfirm.setEnabled(true);
                btnConfirm.setText("XÁC NHẬN & THANH TOÁN");
                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}
```

### 5.2 BookingScheduleActivity — ô đỏ chỉ khi "confirmed"

```java
// activity/booking/BookingScheduleActivity.java — loadBookedSlots()

private void loadBookedSlots() {
    FirebaseFirestore.getInstance()
        .collection("Bookings")
        .whereEqualTo("courtId", court.getCourtId())
        .whereEqualTo("date", selectedDate)
        .get()
        .addOnSuccessListener(snap -> {
            for (var doc : snap.getDocuments()) {
                String status = doc.getString("status");

                // Chỉ block slot khi đã thanh toán xong
                // "awaiting_payment" KHÔNG block (15 phút timeout tự hủy)
                if (!"confirmed".equals(status)) continue;

                // ... đánh dấu bookedMap như cũ ...
            }
            buildGrid();
        });
}
```

**Lý do:** Nếu block slot ngay khi "awaiting_payment", người khác sẽ thấy slot đỏ nhưng thực ra người đặt chưa trả tiền và có thể timeout. Slot chỉ thật sự bị chiếm khi tiền đã về.

---

## 6. Màn hình thanh toán

### 6.1 PaymentActivity.java

**File mới:** `activity/booking/PaymentActivity.java`

```java
public class PaymentActivity extends AppCompatActivity {

    private String bookingId;
    private double depositAmount;
    private double totalAmount;
    private String courtName, bookingDate, customerName;

    // Views
    private ImageView imgQrCode;
    private TextView tvDepositAmount, tvRemainingAmount;
    private TextView tvCountdown;
    private TextView tvBankName, tvAccountNo, tvAccountName, tvTransferContent;
    private MaterialButton btnDonePayment, btnCancelPayment;
    private LinearLayout layoutQrLoading, layoutQrContent;

    private CountDownTimer countDownTimer;
    private ListenerRegistration bookingListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        bookingId     = getIntent().getStringExtra("bookingId");
        depositAmount = getIntent().getDoubleExtra("depositAmount", 0);
        totalAmount   = getIntent().getDoubleExtra("totalAmount", 0);
        courtName     = getIntent().getStringExtra("courtName");
        bookingDate   = getIntent().getStringExtra("bookingDate");
        customerName  = getIntent().getStringExtra("customerName");

        initViews();
        bindStaticInfo();

        // Gọi Cloud Function để lấy QR
        fetchPaymentQR();

        // Lắng nghe Firestore — khi webhook cập nhật status = "confirmed"
        listenForPaymentConfirmation();

        // Bắt đầu đếm ngược 15 phút
        startCountdown(15 * 60 * 1000);
    }

    private void fetchPaymentQR() {
        layoutQrLoading.setVisibility(View.VISIBLE);
        layoutQrContent.setVisibility(View.GONE);

        // Gọi Firebase Cloud Function
        Map<String, Object> data = new HashMap<>();
        data.put("bookingId",    bookingId);
        data.put("amount",       totalAmount);
        data.put("customerName", customerName);
        data.put("courtName",    courtName);
        data.put("bookingDate",  bookingDate);

        // Dùng OkHttp hoặc Retrofit gọi Cloud Function URL
        // URL: https://YOUR_REGION-YOUR_PROJECT.cloudfunctions.net/createPaymentLink
        String functionUrl = "https://asia-southeast1-YOUR_PROJECT.cloudfunctions.net/createPaymentLink";

        RequestBody body = RequestBody.create(
            new Gson().toJson(data),
            MediaType.get("application/json")
        );

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
            .url(functionUrl)
            .post(body)
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject obj = new JSONObject(json);
                        if (obj.getBoolean("success")) {
                            String qrCode = obj.getString("qrCode"); // base64 image hoặc URL
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
                runOnUiThread(() -> showQRError());
            }
        });
    }

    private void displayQR(String qrCodeData) {
        layoutQrLoading.setVisibility(View.GONE);
        layoutQrContent.setVisibility(View.VISIBLE);

        // Nếu qrCode là URL ảnh → dùng Glide
        Glide.with(this).load(qrCodeData).into(imgQrCode);

        // Nếu cần generate QR từ string → dùng ZXing
        // BarcodeEncoder encoder = new BarcodeEncoder();
        // Bitmap bitmap = encoder.encodeBitmap(qrCodeData, BarcodeFormat.QR_CODE, 400, 400);
        // imgQrCode.setImageBitmap(bitmap);
    }

    private void listenForPaymentConfirmation() {
        bookingListener = FirebaseFirestore.getInstance()
            .collection("Bookings")
            .document(bookingId)
            .addSnapshotListener((snap, err) -> {
                if (snap == null) return;
                String status        = snap.getString("status");
                String paymentStatus = snap.getString("paymentStatus");

                if ("confirmed".equals(status) && "paid".equals(paymentStatus)) {
                    // Thanh toán thành công!
                    openSuccessScreen();
                } else if (status != null && status.startsWith("cancelled")) {
                    // Bị hủy (timeout, lỗi...)
                    openCancelledScreen();
                }
            });
    }

    private void startCountdown(long millisInFuture) {
        countDownTimer = new CountDownTimer(millisInFuture, 1000) {
            @Override
            public void onTick(long msLeft) {
                long minutes = msLeft / 60000;
                long seconds = (msLeft % 60000) / 1000;
                tvCountdown.setText(String.format("%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvCountdown.setText("00:00");
                // Booking đã bị Cloud Function hủy
                // Listener sẽ bắt status = "cancelled_timeout"
                Toast.makeText(PaymentActivity.this,
                    "Đã hết thời gian thanh toán!", Toast.LENGTH_LONG).show();
            }
        }.start();
    }

    private void openSuccessScreen() {
        if (bookingListener != null) bookingListener.remove();
        if (countDownTimer  != null) countDownTimer.cancel();
        Intent intent = new Intent(this, BookingSuccessActivity.class);
        intent.putExtra("bookingId", bookingId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void cancelBooking() {
        FirebaseFirestore.getInstance()
            .collection("Bookings").document(bookingId)
            .update("status", "cancelled_by_user", "cancelledBy", "user",
                    "cancelledAt", System.currentTimeMillis());
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bookingListener != null) bookingListener.remove();
        if (countDownTimer  != null) countDownTimer.cancel();
    }
}
```

### 6.2 Layout: `activity_payment.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/bg_primary">

    <!-- Header -->
    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@color/green_primary"
        app:elevation="0dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:paddingTop="48dp"
            android:paddingBottom="16dp"
            android:paddingHorizontal="20dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Thanh toán đặt cọc"
                android:textSize="20sp"
                android:textStyle="bold"
                android:textColor="@color/white"/>

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Quét QR để hoàn tất đặt sân"
                android:textSize="13sp"
                android:textColor="@color/white_80"
                android:layout_marginTop="2dp"/>
        </LinearLayout>
    </com.google.android.material.appbar.AppBarLayout>

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:paddingBottom="100dp"
        android:clipToPadding="false"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <!-- Countdown timer -->
            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                app:cardCornerRadius="12dp"
                app:cardElevation="0dp"
                app:cardBackgroundColor="#FFF3CD"
                android:layout_marginBottom="12dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:gravity="center_vertical"
                    android:padding="14dp">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_weight="1"
                        android:layout_height="wrap_content"
                        android:text="⏱️ Thời gian còn lại"
                        android:textSize="14sp"
                        android:textColor="@color/warning_text"/>

                    <TextView
                        android:id="@+id/tvCountdown"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="15:00"
                        android:textSize="20sp"
                        android:textStyle="bold"
                        android:textColor="@color/warning_text"/>
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <!-- Số tiền -->
            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                app:cardCornerRadius="16dp"
                app:cardElevation="0dp"
                app:strokeColor="@color/divider"
                app:strokeWidth="1dp"
                app:cardBackgroundColor="@color/bg_white"
                android:layout_marginBottom="12dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:layout_marginBottom="8dp">

                        <TextView
                            android:layout_width="0dp"
                            android:layout_weight="1"
                            android:layout_height="wrap_content"
                            android:text="Tiền cọc (30%)"
                            android:textSize="15sp"
                            android:textColor="@color/text_primary"/>

                        <TextView
                            android:id="@+id/tvDepositAmount"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="90.000đ"
                            android:textSize="18sp"
                            android:textStyle="bold"
                            android:textColor="@color/green_dark"/>
                    </LinearLayout>

                    <View
                        android:layout_width="match_parent"
                        android:layout_height="0.5dp"
                        android:background="@color/divider"
                        android:layout_marginBottom="8dp"/>

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal">

                        <TextView
                            android:layout_width="0dp"
                            android:layout_weight="1"
                            android:layout_height="wrap_content"
                            android:text="Thanh toán tại sân (70%)"
                            android:textSize="13sp"
                            android:textColor="@color/text_secondary"/>

                        <TextView
                            android:id="@+id/tvRemainingAmount"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="210.000đ"
                            android:textSize="13sp"
                            android:textColor="@color/text_secondary"/>
                    </LinearLayout>
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <!-- QR Code -->
            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                app:cardCornerRadius="16dp"
                app:cardElevation="0dp"
                app:strokeColor="@color/divider"
                app:strokeWidth="1dp"
                app:cardBackgroundColor="@color/bg_white"
                android:layout_marginBottom="12dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:gravity="center_horizontal"
                    android:padding="20dp">

                    <!-- Loading state -->
                    <LinearLayout
                        android:id="@+id/layoutQrLoading"
                        android:layout_width="match_parent"
                        android:layout_height="260dp"
                        android:orientation="vertical"
                        android:gravity="center"
                        android:visibility="visible">

                        <ProgressBar
                            android:layout_width="48dp"
                            android:layout_height="48dp"
                            android:indeterminateTint="@color/green_primary"/>

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Đang tạo QR..."
                            android:textSize="14sp"
                            android:textColor="@color/text_secondary"
                            android:layout_marginTop="12dp"/>
                    </LinearLayout>

                    <!-- QR loaded -->
                    <LinearLayout
                        android:id="@+id/layoutQrContent"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:gravity="center_horizontal"
                        android:visibility="gone">

                        <ImageView
                            android:id="@+id/imgQrCode"
                            android:layout_width="240dp"
                            android:layout_height="240dp"
                            android:scaleType="fitCenter"/>

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Quét bằng app ngân hàng bất kỳ"
                            android:textSize="13sp"
                            android:textColor="@color/text_secondary"
                            android:layout_marginTop="10dp"/>
                    </LinearLayout>
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <!-- Thông tin chuyển khoản thủ công -->
            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                app:cardCornerRadius="16dp"
                app:cardElevation="0dp"
                app:strokeColor="@color/divider"
                app:strokeWidth="1dp"
                app:cardBackgroundColor="@color/bg_white"
                android:layout_marginBottom="16dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Hoặc chuyển khoản thủ công"
                        android:textSize="13sp"
                        android:textStyle="bold"
                        android:textColor="@color/text_primary"
                        android:layout_marginBottom="10dp"/>

                    <!-- Các dòng thông tin ngân hàng — bind từ PayOS response -->
                    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                        android:orientation="horizontal" android:layout_marginBottom="6dp">
                        <TextView android:layout_width="100dp" android:layout_height="wrap_content"
                            android:text="Ngân hàng" android:textSize="13sp" android:textColor="@color/text_secondary"/>
                        <TextView android:id="@+id/tvBankName" android:layout_width="match_parent"
                            android:layout_height="wrap_content" android:text="Vietcombank"
                            android:textSize="13sp" android:textStyle="bold" android:textColor="@color/text_primary"/>
                    </LinearLayout>

                    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                        android:orientation="horizontal" android:layout_marginBottom="6dp">
                        <TextView android:layout_width="100dp" android:layout_height="wrap_content"
                            android:text="Số tài khoản" android:textSize="13sp" android:textColor="@color/text_secondary"/>
                        <TextView android:id="@+id/tvAccountNo" android:layout_width="match_parent"
                            android:layout_height="wrap_content" android:text="1234567890"
                            android:textSize="13sp" android:textStyle="bold" android:textColor="@color/text_primary"/>
                    </LinearLayout>

                    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                        android:orientation="horizontal" android:layout_marginBottom="6dp">
                        <TextView android:layout_width="100dp" android:layout_height="wrap_content"
                            android:text="Tên TK" android:textSize="13sp" android:textColor="@color/text_secondary"/>
                        <TextView android:id="@+id/tvAccountName" android:layout_width="match_parent"
                            android:layout_height="wrap_content" android:text="CONG TY PICKLEBOOK"
                            android:textSize="13sp" android:textStyle="bold" android:textColor="@color/text_primary"/>
                    </LinearLayout>

                    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                        android:orientation="horizontal">
                        <TextView android:layout_width="100dp" android:layout_height="wrap_content"
                            android:text="Nội dung" android:textSize="13sp" android:textColor="@color/text_secondary"/>
                        <TextView android:id="@+id/tvTransferContent" android:layout_width="0dp"
                            android:layout_weight="1" android:layout_height="wrap_content"
                            android:text="Coc san ABC123"
                            android:textSize="13sp" android:textStyle="bold" android:textColor="@color/green_dark"/>
                    </LinearLayout>
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

        </LinearLayout>
    </androidx.core.widget.NestedScrollView>

    <!-- Nút hủy ở dưới -->
    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnCancelPayment"
        android:layout_width="match_parent"
        android:layout_height="52dp"
        android:layout_gravity="bottom"
        android:layout_margin="16dp"
        android:text="Hủy đặt sân"
        android:textAllCaps="false"
        android:textColor="@color/error_red"
        style="@style/Widget.PickleBall.Button.Outline"
        app:strokeColor="@color/error_red"/>

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

### 6.3 BookingSuccessActivity.java

```java
// Màn hình đơn giản sau khi thanh toán thành công
// Hiển thị:
// - Icon checkmark animation
// - "Đặt sân thành công!"
// - Thông tin tóm tắt: tên sân, ngày, giờ
// - Nhắc nhở: "Vui lòng thanh toán 70% còn lại tại sân"
// - Nút "Xem lịch đặt" → MyBookingsFragment
// - Nút "Về trang chủ"
```

---

## 7. Xử lý Webhook

### 7.1 Cấu hình Webhook trên PayOS Dashboard

Vào https://my.payos.vn → Webhook:
- Webhook URL: `https://asia-southeast1-YOUR_PROJECT.cloudfunctions.net/payosWebhook`
- Secret Key: dùng để verify signature

### 7.2 Test Webhook local

```bash
# Dùng ngrok để test local
ngrok http 5001
# Sau đó set URL ngrok vào PayOS dashboard để test
```

### 7.3 Kiểm tra Webhook hoạt động

PayOS dashboard có nút "Send Test" để gửi event test. Log trong Firebase Functions Console.

---

## 8. Màn hình quản lý chủ sân

### Chủ sân không còn approve/reject booking nữa. Thay vào đó:

### 8.1 OwnerBookingsFragment — đổi thành lịch xem

```java
// Chủ sân chỉ cần xem: hôm nay có ai đặt giờ nào, tên gì, số điện thoại
// KHÔNG có nút Xác nhận / Từ chối nữa

// Thêm tab theo ngày (hôm nay, ngày mai, tuần này)
// Hiển thị thông tin:
// - Tên khách
// - Số điện thoại
// - Slot đã đặt
// - Đã cọc bao nhiêu
// - Còn lại phải thu tại sân
```

### 8.2 Chủ sân hủy booking

Nếu chủ sân cần hủy (sân bị sự cố, bảo trì...) thì:

```java
// OwnerBookingsFragment — thêm nút "Hủy lịch" cho từng booking
private void ownerCancelBooking(Booking booking) {
    new AlertDialog.Builder(requireContext())
        .setTitle("Hủy lịch đặt")
        .setMessage("Hủy lịch của khách " + booking.getCustomerName() + "?\nKhách sẽ được hoàn 100% tiền cọc.")
        .setPositiveButton("Xác nhận hủy", (d, w) -> {
            FirebaseFirestore.getInstance()
                .collection("Bookings").document(booking.getBookingId())
                .update(
                    "status",      "cancelled_by_owner",
                    "cancelledBy", "owner",
                    "cancelledAt", System.currentTimeMillis(),
                    "refundStatus", "refund_pending",  // Admin sẽ hoàn tiền thủ công
                    "refundAmount", booking.getDepositAmount()
                );
            // Gửi notification cho khách
            NotificationHelper.sendBookingCancelledByOwner(
                booking.getUserId(),
                booking.getCourtName(),
                booking.getDate(),
                booking.getBookingId()
            );
        })
        .setNegativeButton("Giữ lại", null)
        .show();
}
```

### 8.3 Thêm NotificationHelper method mới

```java
// utils/NotificationHelper.java — thêm method

public static void sendBookingCancelledByOwner(String customerId, String courtName,
                                                String date, String bookingId) {
    send(new Notification(
        customerId,
        "❌ Lịch đặt bị hủy bởi chủ sân",
        "Lịch đặt sân " + courtName + " ngày " + date +
        " đã bị hủy. Tiền cọc sẽ được hoàn lại trong 1-3 ngày làm việc.",
        "booking_cancelled_by_owner",
        bookingId
    ));
}
```

---

## 9. Màn hình Admin

Admin quản lý 2 việc liên quan đến thanh toán:

### 9.1 Danh sách yêu cầu hoàn tiền

```java
// fragment/admin/AdminRefundFragment.java — màn hình mới

// Query: Bookings có refundStatus = "refund_pending"
// Hiển thị:
// - Tên khách + SĐT
// - Số tiền cần hoàn
// - Lý do hủy
// - Nút "Đã hoàn tiền" → update refundStatus = "refund_done"
// - Nút "Từ chối hoàn" → update refundStatus = "refund_rejected" + nhập lý do
```

### 9.2 Thống kê doanh thu

```java
// fragment/admin/AdminDashboardFragment.java — thêm stats

// Tổng tiền cọc đã thu hôm nay:
// Query Bookings status = "confirmed", paidAt trong hôm nay
// Sum depositAmount

// Tổng yêu cầu hoàn tiền đang chờ:
// Query Bookings refundStatus = "refund_pending"
// Count + Sum refundAmount
```

---

## 10. Edge Cases cần xử lý

### 10.1 Thanh toán xong nhưng slot bị người khác book trong lúc chờ

**Tình huống:** User A và User B cùng xem slot trống. A tạo booking "awaiting_payment". B tạo booking "awaiting_payment" cho cùng slot. A thanh toán trước → confirmed. B thanh toán sau → cũng confirmed nhưng slot đã có người.

**Giải pháp:** Trong `payosWebhook.js`, trước khi update status = "confirmed", kiểm tra lại không có booking confirmed nào trùng slot:

```javascript
// payosWebhook.js — thêm kiểm tra conflict
const conflictSnap = await admin.firestore()
    .collection('Bookings')
    .where('courtId',   '==', booking.courtId)
    .where('date',      '==', booking.date)
    .where('status',    '==', 'confirmed')
    .where('timeSlotId','==', booking.timeSlotId)
    .get();

if (!conflictSnap.empty) {
    // Có conflict → không confirm, hoàn tiền
    await bookingDoc.ref.update({
        status:       'cancelled_conflict',
        refundStatus: 'refund_pending',
        refundAmount:  booking.depositAmount
    });
    // Notify khách
} else {
    // Không conflict → confirm bình thường
    await bookingDoc.ref.update({ status: 'confirmed', paymentStatus: 'paid' });
}
```

### 10.2 Khách đã trả tiền rồi muốn hủy

```java
// MyBookingsFragment.java — showCancelDialog() — thêm logic

private void showCancelDialog(Booking booking) {
    // Tính thời gian còn lại trước khi chơi
    // booking.date + booking.startTime → parse thành timestamp
    long playTimeMs    = parseBookingDateTime(booking.getDate(), booking.getStartTime());
    long msUntilPlay   = playTimeMs - System.currentTimeMillis();
    boolean canRefund  = msUntilPlay >= Constants.FREE_CANCEL_WINDOW_MS;

    String message = canRefund
        ? "Hủy lịch sân \"" + booking.getCourtName() + "\" vào ngày " + booking.getDate()
          + "?\nBạn sẽ được hoàn 100% tiền cọc (" + formatMoney(booking.getDepositAmount()) + ")."
        : "Hủy lịch sân \"" + booking.getCourtName() + "\" vào ngày " + booking.getDate()
          + "?\n⚠️ Còn dưới 2 tiếng trước giờ chơi — bạn sẽ MẤT tiền cọc "
          + formatMoney(booking.getDepositAmount()) + ".";

    new AlertDialog.Builder(requireContext())
        .setTitle("Hủy đặt sân")
        .setMessage(message)
        .setPositiveButton("Xác nhận hủy", (d, w) -> cancelBooking(booking, canRefund))
        .setNegativeButton("Giữ lại", null)
        .show();
}

private void cancelBooking(Booking booking, boolean shouldRefund) {
    Map<String, Object> updates = new HashMap<>();
    updates.put("status",        "cancelled_by_user");
    updates.put("cancelledBy",   "user");
    updates.put("cancelledAt",   System.currentTimeMillis());
    updates.put("refundStatus",  shouldRefund ? "refund_pending" : "not_applicable");
    updates.put("refundAmount",  shouldRefund ? booking.getDepositAmount() : 0);

    FirebaseFirestore.getInstance()
        .collection("Bookings").document(booking.getBookingId())
        .update(updates);
}
```

### 10.3 No-show (không tới, không hủy)

Cloud Function chạy hàng ngày lúc 23:00, tìm booking `confirmed` có `date` = hôm nay và `endTime` đã qua, update thành `cancelled_no_show`. Không hoàn tiền.

```javascript
// Thêm vào cancelExpiredBookings.js hoặc tạo file riêng
exports.markNoShowBookings = functions.pubsub
    .schedule('0 23 * * *')  // 23:00 mỗi ngày
    .timeZone('Asia/Ho_Chi_Minh')
    .onRun(async (context) => {
        const today    = getTodayString(); // "dd/MM/yyyy"
        const nowHHMM  = getCurrentTime(); // "HH:mm"

        const snap = await admin.firestore()
            .collection('Bookings')
            .where('status', '==', 'confirmed')
            .where('date',   '==', today)
            .get();

        const batch = admin.firestore().batch();
        snap.docs.forEach(doc => {
            if (doc.data().endTime < nowHHMM) {
                batch.update(doc.ref, {
                    status:       'completed',  // hoặc giữ confirmed, tùy UX
                    paymentStatus: doc.data().paymentStatus
                });
            }
        });
        await batch.commit();
    });
```

---

## 11. Thứ tự thực hiện

### Sprint 1 — Nền tảng (không có UI)
- [ ] Tạo Firebase Functions project
- [ ] Deploy `createPaymentLink.js`
- [ ] Deploy `payosWebhook.js`
- [ ] Deploy `cancelExpiredBookings.js` (cron)
- [ ] Test webhook với PayOS sandbox
- [ ] Cập nhật `Booking.java` model (thêm fields)
- [ ] Cập nhật `Constants.java`
- [ ] Tạo Firestore indexes

### Sprint 2 — Booking flow mới
- [ ] Sửa `BookingScheduleActivity` — chỉ đỏ khi `confirmed`
- [ ] Sửa `BookingConfirmActivity` — tạo booking `awaiting_payment`
- [ ] Tạo `PaymentActivity.java` + layout
- [ ] Tạo `BookingSuccessActivity.java` + layout
- [ ] Test end-to-end: đặt → QR → thanh toán → confirmed

### Sprint 3 — Hủy & hoàn tiền
- [ ] Sửa `MyBookingsFragment` — logic hủy có kiểm tra thời gian
- [ ] Sửa `OwnerBookingsFragment` — bỏ confirm/reject, thêm view-only + cancel
- [ ] Thêm `AdminRefundFragment` — danh sách chờ hoàn tiền
- [ ] Test các case hủy

### Sprint 4 — Polish
- [ ] Xử lý edge case conflict booking
- [ ] No-show cron job
- [ ] Thống kê doanh thu trong Admin dashboard
- [ ] Thêm màn hình lịch sử giao dịch cho khách

---

## Phụ lục: Dependencies cần thêm vào build.gradle

```gradle
// app/build.gradle
dependencies {
    // HTTP client để gọi Cloud Functions
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'
    
    // JSON parsing
    implementation 'com.google.code.gson:gson:2.10.1'
    
    // QR Code generation (nếu cần generate từ string thay vì dùng ảnh từ PayOS)
    implementation 'com.journeyapps:zxing-android-embedded:4.3.0'
}
```

```json
// functions/package.json
{
  "dependencies": {
    "firebase-admin":    "^11.0.0",
    "firebase-functions": "^4.0.0",
    "axios":             "^1.4.0"
  }
}
```

---

## Ghi chú quan trọng cho AI Coder

1. **PayOS sandbox:** Dùng tài khoản sandbox để test, không dùng production key. Trong sandbox, mọi giao dịch đều được chấp nhận không cần thật sự chuyển khoản.

2. **Không bao giờ** để `PAYOS_API_KEY` và `PAYOS_CHECKSUM_KEY` trong Android code. Chỉ để trong Firebase Functions environment config.

3. **Webhook phải là HTTPS** — Firebase Functions mặc định là HTTPS nên không cần lo.

4. **Hoàn tiền giai đoạn đầu là thủ công** — Admin nhận yêu cầu từ app, chuyển khoản tay. Chưa cần tích hợp PayOS refund API.

5. **`payosOrderCode` phải là số nguyên dương và unique.** Dùng `Date.now()` slice lấy 9 chữ số cuối là đủ an toàn cho scale nhỏ.

6. **Test kỹ case:** Thanh toán thành công nhưng Firestore update thất bại → webhook cần retry logic hoặc idempotent check.
