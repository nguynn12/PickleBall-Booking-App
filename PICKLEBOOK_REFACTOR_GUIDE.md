# PickleBook — Hướng dẫn Refactor & Phát triển Tính năng Mới
> **Dành cho:** Claude Sonnet 4.6 (AI Coder)  
> **Dự án:** Android App — Đặt sân Pickleball  
> **Stack:** Java · Firebase Firestore · Firebase Auth · Firebase Storage · Google Maps SDK  
> **Mức độ ưu tiên:** 🔴 Cao | 🟡 Trung bình | 🟢 Thấp

---

## Mục lục
1. [Tổng quan kiến trúc hiện tại](#1-tổng-quan-kiến-trúc-hiện-tại)
2. [BUG & VẤN ĐỀ CẦN SỬA NGAY](#2-bug--vấn-đề-cần-sửa-ngay)
3. [Tab Khám Phá — Thiết kế lại hoàn toàn](#3-tab-khám-phá--thiết-kế-lại-hoàn-toàn)
4. [Tab Bản Đồ — Sửa lỗi và cải thiện](#4-tab-bản-đồ--sửa-lỗi-và-cải-thiện)
5. [Tính năng Tìm Người Chơi (Radar)](#5-tính-năng-tìm-người-chơi-radar)
6. [Cải thiện Code Chung](#6-cải-thiện-code-chung)
7. [Cải thiện UI/UX](#7-cải-thiện-uiux)
8. [Thứ tự thực hiện khuyến nghị](#8-thứ-tự-thực-hiện-khuyến-nghị)

---

## 1. Tổng quan kiến trúc hiện tại

### Cấu trúc Navigation
```
CustomerMainActivity (BottomNav 5 tabs)
├── nav_home       → HomeFragment           ← Hiển thị danh sách sân + search
├── nav_explore    → CourtListFragment      ← TRÙNG với HomeFragment (cần đổi)
├── nav_map        → MapFragment            ← Bản đồ sân (còn lỗi)
├── nav_bookings   → MyBookingsFragment     ← Lịch đặt
└── nav_profile    → ProfileFragment        ← Hồ sơ
```

### Firestore Collections hiện tại
- `Users` — thông tin người dùng
- `Courts` — sân pickleball
- `Courts/{id}/SubCourts` — sân con
- `Courts/{id}/Services` — bảng giá & dịch vụ
- `Bookings` — đặt sân
- `Reviews` — đánh giá
- `Notifications` — thông báo trong app

### Collections cần thêm (cho tính năng mới)
- `PlayerSessions` — phiên tìm người chơi (matchmaking)

---

## 2. BUG & VẤN ĐỀ CẦN SỬA NGAY

### 🔴 BUG-01: Tab Khám Phá trùng với Trang Chủ
**File:** `fragment/customer/CourtListFragment.java`  
**Vấn đề:** `CourtListFragment` và `HomeFragment` đều hiển thị danh sách sân + search bar → UX tệ, người dùng bối rối.  
**Hướng xử lý:** Xem mục [3. Tab Khám Phá — Thiết kế lại hoàn toàn](#3-tab-khám-phá--thiết-kế-lại-hoàn-toàn)

---

### 🔴 BUG-02: MapFragment — Bản đồ không hoạt động đúng
**File:** `fragment/customer/MapFragment.java`  
**Các lỗi cụ thể:**

**2a. `DEFAULT_CENTER` hardcode tọa độ Đà Lạt**
```java
// Hiện tại (SAI khi test ở nơi khác)
private static final LatLng DEFAULT_CENTER = new LatLng(11.940419, 108.458313);
```
→ Nên dùng location thực của user, fallback mới là Đà Lạt.

**2b. `suppressAutoFitBounds` — logic lộn xộn**
```java
// Flag này bị set ở nhiều nơi gây race condition
suppressAutoFitBounds = moveCamera; // trong onDeviceLocationAvailable
suppressAutoFitBounds = false;      // cuối loadNearbyCourtsAndRender
```
→ Cần refactor thành 1 luồng rõ ràng: lấy location → load courts → render markers → fit bounds nếu cần.

**2c. Không xử lý khi không có location permission**  
Khi user từ chối permission, bản đồ chỉ move camera về `DEFAULT_CENTER` nhưng markers không load vì `currentLatLng == null`.
```java
// Trong loadNearbyCourtsAndRender()
if (googleMap == null || currentLatLng == null) return; // ← bỏ qua load nếu không có GPS
```
→ Cần load **tất cả sân** khi không có GPS, không lọc theo khoảng cách.

**2d. `BottomSheetBehavior` — peekHeight quá nhỏ / trạng thái ban đầu ẩn**  
```java
bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN); // ẩn ngay khi vào tab
```
→ Nên `STATE_COLLAPSED` mặc định để user biết có danh sách sân.

**2e. `onDestroyView` destroy MapView nhưng không check null đúng cách**
```java
@Override
public void onDestroyView() {
    if (mapView != null) mapView.onDestroy(); // ← gọi onDestroy() thay vì phải dùng onDestroyView lifecycle
    mapView = null;
    googleMap = null;
    ...
}
```
→ Fragment lifecycle cho MapView phải map đúng: `onDestroyView` không nên gọi `mapView.onDestroy()` — chỉ gọi `onDestroy()` khi fragment thực sự bị destroy. Dùng `ViewLifecycleOwner` hoặc tách thành Activity riêng.

**2f. Marker click không pan camera đến court**
```java
googleMap.setOnMarkerClickListener(marker -> {
    Court court = markerCourtMap.get(marker);
    if (court == null) return false;
    openCourtDetailSheet(court); // ← mở sheet nhưng không animateCamera
    return false;
});
```
→ Thêm `googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 15f))` trước khi mở sheet.

---

### 🟡 BUG-03: BookingAdapter — Rating hardcode
**File:** `adapter/CourtAdapter.java`
```java
holder.tvRating.setText("⭐ 5.0"); // ← hardcode, không lấy từ Firestore
```
→ Cần query collection `Reviews` cho từng court hoặc lưu `avgRating` field vào Court document.

---

### 🟡 BUG-04: Booking không lưu `subCourtId` đúng field
**File:** `activity/booking/BookingConfirmActivity.java`
```java
b.setTimeSlotId(subCourtId); // ← lưu subCourtId vào field timeSlotId (legacy, nhầm tên)
```
**File:** `activity/booking/BookingScheduleActivity.java` — query dùng `subCourtId` nhưng model lưu vào `timeSlotId`.  
→ Thêm field `subCourtId` vào `Booking` model và dùng nhất quán.

---

### 🟡 BUG-05: ProfileFragment inflate layout Activity
**File:** `fragment/ProfileFragment.java`
```java
return inflater.inflate(R.layout.activity_profile, container, false); // ← dùng activity layout cho fragment
```
→ Nên tạo `fragment_profile.xml` riêng hoặc đảm bảo layout `activity_profile.xml` tương thích khi dùng trong Fragment (hiện tại có thể gây lỗi padding status bar).

---

### 🟡 BUG-06: SplashActivity — private method gọi static method
**File:** `activity/auth/SplashActivity.java`
```java
private void checkRoleAndNavigate(String uid) {
    ...
    navigateByRole(role); // ← gọi instance method nhưng navigateByRole(String) là private
}
```
→ `navigateByRole(String)` gọi `navigateByRole(this, role)` — okay, nhưng nên đổi tên rõ hơn để tránh nhầm với static method.

---

### 🟢 BUG-07: MainActivity — test code còn sót lại
**File:** `activity/MainActivity.java`  
→ Cả file này chỉ là test ghi Firestore. Xóa hoặc disable hoàn toàn trước khi release.

---

## 3. Tab Khám Phá — Thiết kế lại hoàn toàn

### Mục tiêu
Tab "Khám phá" (`nav_explore`) sẽ **không còn là danh sách sân** nữa, mà trở thành **màn hình tìm người chơi xung quanh dựa vào GPS**.

### Layout mới: `fragment_court_list.xml` → đổi thành `fragment_explore.xml`

```xml
<!-- Cấu trúc layout mới -->
<CoordinatorLayout>
    <!-- Header xanh -->
    <AppBarLayout>
        <LinearLayout>
            <TextView android:text="Xung quanh bạn" />
            <TextView android:text="Người chơi pickleball gần đây" />
        </LinearLayout>
    </AppBarLayout>

    <!-- Body: danh sách người chơi gần -->
    <RecyclerView android:id="@+id/rvNearbyPlayers" />

    <!-- Empty state khi không có ai -->
    <LinearLayout android:id="@+id/layoutEmpty">
        <TextView android:text="Chưa có người chơi nào gần bạn" />
        <TextView android:text="Hãy bật tìm kiếm để kết nối!" />
    </LinearLayout>

    <!-- Nút CỐ ĐỊNH ở dưới cùng -->
    <MaterialButton
        android:id="@+id/btnFindPlayer"
        android:layout_gravity="bottom"
        android:layout_margin="16dp"
        android:text="🏓 Tìm người chơi"
        android:layout_height="56dp" />
</CoordinatorLayout>
```

### Java mới: `CourtListFragment.java` → đổi thành `ExploreFragment.java`

```java
public class ExploreFragment extends Fragment {

    private static final float NEARBY_RADIUS_METERS = 5000f; // 5km
    private RecyclerView rvNearbyPlayers;
    private NearbyPlayerAdapter adapter;
    private List<User> nearbyPlayerList = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng myLatLng;

    @Override
    public View onCreateView(...) {
        return inflater.inflate(R.layout.fragment_explore, container, false);
    }

    @Override
    public void onViewCreated(...) {
        // Setup RecyclerView danh sách người chơi gần
        setupRecyclerView(view);
        
        // Xin permission GPS → load người chơi gần
        requestLocationAndLoadPlayers();
        
        // Nút tìm người chơi → mở RadarActivity
        view.findViewById(R.id.btnFindPlayer).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), RadarMatchActivity.class);
            startActivity(intent);
        });
    }

    private void requestLocationAndLoadPlayers() {
        // Dùng FusedLocationProviderClient để lấy GPS
        // Sau khi có location → query Firestore Users có lat/lng trong radius
        // Hiển thị vào RecyclerView
    }

    private void loadNearbyPlayers(LatLng myLocation) {
        // Query Users collection
        // Lọc theo khoảng cách < NEARBY_RADIUS_METERS
        // Bỏ qua user hiện tại
        // Update nearbyPlayerList + notifyDataSetChanged
    }
}
```

### Model mới: `PlayerSession.java`
```java
// app/src/main/java/com/example/pickleball/model/PlayerSession.java
public class PlayerSession {
    private String sessionId;
    private String userId;
    private String userName;
    private String avatarUrl;
    private String skillLevel;    // "beginner" | "intermediate" | "pro"
    private double lat;
    private double lng;
    private String status;        // "searching" | "matched" | "expired"
    private long createdAt;
    private long expiresAt;       // createdAt + 5 phút (300000ms)
    private String matchedWith;   // userId của người được ghép
    // getters & setters...
}
```

### Adapter mới: `NearbyPlayerAdapter.java`
```java
// Hiển thị danh sách người chơi gần:
// - Avatar (chữ cái đầu hoặc ảnh)
// - Tên + skill level badge
// - Khoảng cách (ví dụ: "1.2km")
// - Nút "Thách đấu" (tùy chọn, tính năng tương lai)
```

### Cập nhật `CustomerMainActivity.java`
```java
// Đổi từ CourtListFragment sang ExploreFragment
} else if (id == R.id.nav_explore) {
    loadFragment(new ExploreFragment()); // ← đổi đây
    return true;
}
```

### Cập nhật Firestore: thêm lat/lng vào User
Hiện tại User document không có `lat`/`lng`. Cần:
1. Khi user mở ExploreFragment → lấy GPS → update `lat`, `lng`, `lastSeen` vào Firestore Users document.
2. Query nearby players dựa vào Firestore (không có geohash nên phải client-side filter hoặc dùng Geofire).

**Giải pháp đơn giản (không cần Geofire):**
```java
// Lấy tất cả Users có status "online" trong 10 phút gần nhất
FirebaseFirestore.getInstance()
    .collection("Users")
    .whereGreaterThan("lastSeen", System.currentTimeMillis() - 600_000)
    .get()
    .addOnSuccessListener(snap -> {
        // Filter client-side theo khoảng cách
        for (doc : snap) {
            double lat = doc.getDouble("lat");
            double lng = doc.getDouble("lng");
            // tính khoảng cách, nếu < radius thì thêm vào list
        }
    });
```

---

## 4. Tab Bản Đồ — Sửa lỗi và cải thiện

### Vấn đề chính cần sửa

#### 4.1 Sửa lifecycle MapView trong Fragment
**Vấn đề:** Gọi `mapView.onDestroy()` trong `onDestroyView()` khiến crash khi navigate back.

**Sửa đúng lifecycle:**
```java
// Trong MapFragment.java

@Override
public void onResume() {
    super.onResume();
    if (mapView != null) mapView.onResume();
}

@Override
public void onPause() {
    if (mapView != null) mapView.onPause();
    super.onPause();
}

@Override
public void onStart() {
    super.onStart();
    if (mapView != null) mapView.onStart();
}

@Override
public void onStop() {
    if (mapView != null) mapView.onStop();
    super.onStop();
}

@Override
public void onDestroyView() {
    // KHÔNG gọi mapView.onDestroy() ở đây
    // Chỉ null reference để GC
    googleMap = null;
    // mapView giữ nguyên để lifecycle tiếp tục đúng
    super.onDestroyView();
}

@Override
public void onDestroy() {
    if (mapView != null) {
        mapView.onDestroy(); // ← gọi ở đây mới đúng
        mapView = null;
    }
    super.onDestroy();
}

@Override
public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    if (mapView != null) mapView.onSaveInstanceState(outState);
}

@Override
public void onLowMemory() {
    super.onLowMemory();
    if (mapView != null) mapView.onLowMemory();
}
```

#### 4.2 Load tất cả sân khi không có GPS
```java
private void loadCourtsAndRender(@Nullable LatLng userLocation) {
    new FirebaseHelper().getAllCourts(task -> {
        if (!task.isSuccessful() || task.getResult() == null) return;
        
        googleMap.clear();
        markerCourtMap.clear();
        nearbyCourts.clear();
        
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasAny = false;
        
        for (QueryDocumentSnapshot doc : task.getResult()) {
            Double lat = doc.getDouble("lat");
            Double lng = doc.getDouble("lng");
            if (lat == null || lng == null) continue; // bỏ qua sân chưa có tọa độ
            
            // Nếu có GPS → lọc theo khoảng cách; nếu không → show tất cả
            if (userLocation != null) {
                float[] dist = new float[1];
                Location.distanceBetween(userLocation.latitude, userLocation.longitude, lat, lng, dist);
                if (dist[0] > SEARCH_RADIUS_METERS) continue;
            }
            
            Court court = doc.toObject(Court.class);
            if (court.getCourtId() == null) court.setCourtId(doc.getId());
            court.setLat(lat);
            court.setLng(lng);
            
            LatLng courtLatLng = new LatLng(lat, lng);
            Marker marker = googleMap.addMarker(new MarkerOptions()
                .position(courtLatLng)
                .title(court.getCourtName()));
            if (marker != null) markerCourtMap.put(marker, court);
            
            nearbyCourts.add(court);
            boundsBuilder.include(courtLatLng);
            hasAny = true;
        }
        
        // Fit bounds
        if (hasAny && googleMap != null) {
            try {
                if (userLocation != null) {
                    boundsBuilder.include(userLocation);
                }
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
            } catch (Exception e) {
                LatLng center = userLocation != null ? userLocation : DEFAULT_CENTER;
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, DEFAULT_ZOOM));
            }
        }
        
        applySearchFilter(edtSearch == null ? "" : edtSearch.getText().toString());
    });
}
```

#### 4.3 BottomSheet mặc định collapsed
```java
// Trong onCreateView, sau khi setup bottomSheetBehavior:
bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED); // ← thay STATE_HIDDEN
```

#### 4.4 Camera pan khi click marker
```java
googleMap.setOnMarkerClickListener(marker -> {
    Court court = markerCourtMap.get(marker);
    if (court == null) return false;
    
    // Pan camera đến marker
    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 15f));
    
    // Mở detail sheet
    openCourtDetailSheet(court);
    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    return true; // return true để không show default InfoWindow
});
```

#### 4.5 Custom Marker Icon (tùy chọn, cải thiện UX)
```java
// Thêm custom marker màu xanh lá thay vì đỏ mặc định
BitmapDescriptor courtIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);

Marker marker = googleMap.addMarker(new MarkerOptions()
    .position(courtLatLng)
    .title(court.getCourtName())
    .icon(courtIcon));
```

---

## 5. Tính năng Tìm Người Chơi (Radar)

### Luồng tổng thể
```
ExploreFragment
    └── Nút "Tìm người chơi"
            ↓
    RadarMatchActivity  ← màn hình radar quét
        - Animation radar quét vòng tròn
        - Đếm thời gian tìm kiếm (countdown hoặc count up)
        - Lắng nghe Firestore realtime cho PlayerSessions
            ↓ (khi ghép được)
    MatchFoundActivity  ← màn hình ghép cặp thành công
        - Hiển thị thông tin đối thủ
        - Nút "Chấp nhận" / "Từ chối"
        - Đề xuất sân gần nhất
```

### 5.1 RadarMatchActivity

**File mới:** `activity/player/RadarMatchActivity.java`

```java
public class RadarMatchActivity extends AppCompatActivity {

    private static final long SESSION_TIMEOUT_MS = 5 * 60 * 1000; // 5 phút
    
    private FirebaseFirestore db;
    private String currentUserId;
    private String sessionDocId;
    private ListenerRegistration sessionListener;
    
    // Views
    private View radarView;         // custom view hoặc Lottie animation
    private TextView tvTimer;
    private TextView tvStatus;
    private MaterialButton btnCancel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radar_match);
        
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();
        
        // Bắt đầu animation radar
        startRadarAnimation();
        
        // Tạo session tìm kiếm trên Firestore
        createSearchSession();
        
        // Đếm giờ
        startTimer();
        
        btnCancel.setOnClickListener(v -> cancelSearch());
    }
    
    private void createSearchSession() {
        // Lấy GPS hiện tại
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        client.getLastLocation().addOnSuccessListener(location -> {
            double lat = location != null ? location.getLatitude() : 0;
            double lng = location != null ? location.getLongitude() : 0;
            
            // Lấy thông tin user
            db.collection("Users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    PlayerSession session = new PlayerSession();
                    session.setUserId(currentUserId);
                    session.setUserName(doc.getString("fullName"));
                    session.setAvatarUrl(doc.getString("avatarUrl"));
                    session.setSkillLevel(doc.getString("skillLevel"));
                    session.setLat(lat);
                    session.setLng(lng);
                    session.setStatus("searching");
                    session.setCreatedAt(System.currentTimeMillis());
                    session.setExpiresAt(System.currentTimeMillis() + SESSION_TIMEOUT_MS);
                    
                    db.collection("PlayerSessions").add(session)
                        .addOnSuccessListener(ref -> {
                            sessionDocId = ref.getId();
                            ref.update("sessionId", sessionDocId);
                            listenForMatch();
                        });
                });
        });
    }
    
    private void listenForMatch() {
        // Lắng nghe document của session hiện tại
        sessionListener = db.collection("PlayerSessions")
            .document(sessionDocId)
            .addSnapshotListener((snap, err) -> {
                if (snap == null) return;
                String status = snap.getString("status");
                String matchedWith = snap.getString("matchedWith");
                
                if ("matched".equals(status) && matchedWith != null) {
                    // Đã ghép cặp → mở màn hình MatchFound
                    openMatchFound(matchedWith);
                }
            });
        
        // Đồng thời tìm session khác đang tìm kiếm
        tryMatchWithOther();
    }
    
    private void tryMatchWithOther() {
        // Query các session đang "searching" (không phải của mình)
        // Lọc theo khoảng cách và skill level tương đồng
        db.collection("PlayerSessions")
            .whereEqualTo("status", "searching")
            .whereGreaterThan("expiresAt", System.currentTimeMillis())
            .get()
            .addOnSuccessListener(snap -> {
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    String userId = doc.getString("userId");
                    if (userId == null || userId.equals(currentUserId)) continue;
                    
                    // Ghép cặp: cập nhật cả 2 session
                    matchPlayers(doc.getId(), userId);
                    break; // Chỉ ghép với người đầu tiên tìm thấy
                }
            });
    }
    
    private void matchPlayers(String otherSessionId, String otherUserId) {
        WriteBatch batch = db.batch();
        
        // Cập nhật session của mình
        DocumentReference myRef = db.collection("PlayerSessions").document(sessionDocId);
        batch.update(myRef, "status", "matched", "matchedWith", otherUserId);
        
        // Cập nhật session của đối phương
        DocumentReference otherRef = db.collection("PlayerSessions").document(otherSessionId);
        batch.update(otherRef, "status", "matched", "matchedWith", currentUserId);
        
        batch.commit();
    }
    
    private void openMatchFound(String matchedUserId) {
        if (sessionListener != null) sessionListener.remove();
        Intent intent = new Intent(this, MatchFoundActivity.class);
        intent.putExtra("matchedUserId", matchedUserId);
        startActivity(intent);
        finish();
    }
    
    private void cancelSearch() {
        if (sessionDocId != null) {
            db.collection("PlayerSessions").document(sessionDocId)
                .update("status", "cancelled");
        }
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sessionListener != null) sessionListener.remove();
        // Nếu chưa ghép → hủy session
        if (sessionDocId != null) {
            db.collection("PlayerSessions").document(sessionDocId)
                .update("status", "cancelled");
        }
    }
}
```

### 5.2 Layout Radar: `activity_radar_match.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/text_primary">

    <!-- Nút back -->
    <ImageView
        android:id="@+id/btnBack"
        android:layout_width="40dp"
        android:layout_height="40dp"
        android:layout_marginTop="48dp"
        android:layout_marginStart="20dp"
        android:src="@android:drawable/ic_media_previous"
        android:padding="8dp"
        app:tint="@color/white"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"/>

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Tìm người chơi"
        android:textSize="18sp"
        android:textStyle="bold"
        android:textColor="@color/white"
        app:layout_constraintTop_toTopOf="@id/btnBack"
        app:layout_constraintBottom_toBottomOf="@id/btnBack"
        app:layout_constraintStart_toEndOf="@id/btnBack"
        android:layout_marginStart="8dp"/>

    <!-- Radar animation view (custom hoặc dùng Lottie) -->
    <!-- Option A: dùng com.airbnb.lottie:lottie nếu có file JSON -->
    <!-- Option B: custom RadarView extends View với Canvas animation -->
    <com.example.pickleball.view.RadarView
        android:id="@+id/radarView"
        android:layout_width="280dp"
        android:layout_height="280dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginBottom="40dp"/>

    <!-- Timer text -->
    <TextView
        android:id="@+id/tvTimer"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="00:00"
        android:textSize="32sp"
        android:textStyle="bold"
        android:textColor="@color/white"
        app:layout_constraintTop_toBottomOf="@id/radarView"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="16dp"/>

    <TextView
        android:id="@+id/tvStatus"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Đang tìm kiếm..."
        android:textSize="16sp"
        android:textColor="@color/white_60"
        app:layout_constraintTop_toBottomOf="@id/tvTimer"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="8dp"/>

    <!-- Nút hủy -->
    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnCancel"
        android:layout_width="match_parent"
        android:layout_height="52dp"
        android:layout_gravity="bottom"
        android:layout_marginHorizontal="32dp"
        android:layout_marginBottom="48dp"
        android:text="Hủy tìm kiếm"
        android:textAllCaps="false"
        android:textColor="@color/white"
        app:backgroundTint="#44FFFFFF"
        app:cornerRadius="26dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

</androidx.constraintlayout.widget.ConstraintLayout>
```

### 5.3 Custom RadarView (nếu không dùng Lottie)

**File mới:** `view/RadarView.java`

```java
// Custom View vẽ radar:
// 1. 3 vòng tròn đồng tâm màu xanh lá (opacity giảm dần)
// 2. 1 "quét" rotate quanh tâm (arc gradient)
// 3. Điểm trung tâm = user hiện tại
// 4. Các điểm nhỏ random = người chơi gần (nếu có)

public class RadarView extends View {
    private Paint circlePaint, sweepPaint, dotPaint;
    private float sweepAngle = 0f;
    private ValueAnimator animator;
    
    // Khởi tạo Paint objects
    // onDraw: vẽ circles + sweep + dots
    // startAnimation: rotate sweepAngle từ 0 → 360 lặp vô hạn
}
```

### 5.4 MatchFoundActivity

**File mới:** `activity/player/MatchFoundActivity.java`

```java
// Layout: activity_match_found.xml
// Nội dung:
// - Header "🎉 Đã tìm thấy đối thủ!"
// - Card thông tin người ghép:
//   + Avatar
//   + Tên
//   + Skill level badge
//   + Khoảng cách
// - Đề xuất sân gần nhất (load từ Firestore theo GPS)
// - Nút "Đặt sân ngay" → BookingScheduleActivity
// - Nút "Tìm lại" → RadarMatchActivity
// - Auto timeout 30s nếu không phản hồi → quay lại radar
```

### 5.5 Firestore Security Rules cho PlayerSessions
```
// Thêm vào Firestore Rules:
match /PlayerSessions/{sessionId} {
    allow read: if request.auth != null;
    allow create: if request.auth.uid == request.resource.data.userId;
    allow update: if request.auth != null; // cho phép update status khi match
    allow delete: if request.auth.uid == resource.data.userId;
}
```

---

## 6. Cải thiện Code Chung

### 6.1 🔴 Thêm field `subCourtId` vào Booking model

```java
// model/Booking.java — thêm field mới
private String subCourtId;  // ← thêm mới, thay thế việc dùng timeSlotId sai mục đích

public String getSubCourtId() { return subCourtId; }
public void setSubCourtId(String subCourtId) { this.subCourtId = subCourtId; }
```

```java
// activity/booking/BookingConfirmActivity.java — sửa makeBooking()
private Booking makeBooking(...) {
    ...
    b.setSubCourtId(subCourtId); // ← dùng field đúng
    // b.setTimeSlotId(subCourtId); ← xóa dòng này
    return b;
}
```

### 6.2 🟡 Thêm `avgRating` vào Court document

Thay vì query Reviews mỗi lần hiển thị card, lưu sẵn `avgRating` và `reviewCount` vào Court document và cập nhật khi có review mới.

```java
// fragment/court/TabReviewsFragment.java — sau khi submit review
private void submitReview(float rating, String comment) {
    ...
    // Sau khi thêm review thành công, cập nhật avgRating trong Court
    updateCourtRating(court.getCourtId());
}

private void updateCourtRating(String courtId) {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    db.collection("Reviews").whereEqualTo("courtId", courtId).get()
        .addOnSuccessListener(snap -> {
            float total = 0;
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Double r = doc.getDouble("rating");
                if (r != null) total += r;
            }
            float avg = snap.isEmpty() ? 0 : total / snap.size();
            db.collection("Courts").document(courtId)
                .update("avgRating", avg, "reviewCount", snap.size());
        });
}
```

```java
// adapter/CourtAdapter.java — đọc avgRating từ model
double avg = court.getAvgRating(); // thêm field này vào Court model
holder.tvRating.setText(avg > 0 
    ? String.format("⭐ %.1f", avg) 
    : "⭐ Mới");
```

### 6.3 🟡 Thêm lat/lng cho User khi mở ExploreFragment

```java
// Trong ExploreFragment.java
private void updateMyLocation(LatLng latLng) {
    String uid = FirebaseAuth.getInstance().getCurrentUser() != null
        ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    if (uid == null) return;
    
    Map<String, Object> updates = new HashMap<>();
    updates.put("lat", latLng.latitude);
    updates.put("lng", latLng.longitude);
    updates.put("lastSeen", System.currentTimeMillis());
    
    FirebaseFirestore.getInstance()
        .collection("Users").document(uid)
        .update(updates);
}
```

### 6.4 🟢 Cleanup MainActivity.java

```java
// activity/MainActivity.java — xóa toàn bộ test code
// Hoặc chuyển thành redirect sang SplashActivity
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Redirect ngay sang SplashActivity
        startActivity(new Intent(this, SplashActivity.class));
        finish();
    }
}
```

---

## 7. Cải thiện UI/UX

### 7.1 🟡 HomeFragment — Thêm "Sân gợi ý" cho logged-in user

Hiện tại HomeFragment chỉ có search + list. Khi user đã đăng nhập, thêm section:
- **"Sân bạn đã đặt trước đó"** — query Bookings theo userId
- **"Sân gần bạn nhất"** — dùng GPS

### 7.2 🟡 ExploreFragment — Empty state khi không có GPS

```xml
<!-- Thêm vào fragment_explore.xml -->
<LinearLayout android:id="@+id/layoutNoGps" android:visibility="gone">
    <TextView android:text="📍 Cần bật GPS để tìm người chơi" />
    <MaterialButton android:id="@+id/btnEnableGps" android:text="Bật vị trí" />
</LinearLayout>
```

### 7.3 🟢 CourtAdapter — Skeleton loading

Thay vì hiển thị list trống khi đang load từ Firestore, thêm shimmer/skeleton placeholder.

```java
// Dùng thư viện: com.facebook.shimmer:shimmer
// Hoặc tự tạo với View animation
```

### 7.4 🟢 Bottom Navigation Badge

Hiển thị badge số lượng booking đang `pending` trên nav_bookings:

```java
// Trong CustomerMainActivity.java
private void setupNotificationBadge() {
    String uid = FirebaseAuth.getInstance().getCurrentUser() != null
        ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    if (uid == null) return;
    
    FirebaseFirestore.getInstance()
        .collection("Bookings")
        .whereEqualTo("userId", uid)
        .whereEqualTo("status", "pending")
        .addSnapshotListener((snap, err) -> {
            if (snap == null) return;
            int count = snap.size();
            BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_bookings);
            if (count > 0) {
                badge.setVisible(true);
                badge.setNumber(count);
            } else {
                badge.setVisible(false);
            }
        });
}
```

---

## 8. Thứ tự thực hiện khuyến nghị

### Sprint 1 — Sửa bug cốt lõi (ưu tiên cao nhất)
- [ ] **BUG-02:** Sửa MapFragment lifecycle + load courts khi không có GPS
- [ ] **BUG-01:** Đổi CourtListFragment thành ExploreFragment (skeleton, chưa cần full feature)
- [ ] **BUG-04:** Sửa field `subCourtId` trong Booking

### Sprint 2 — Tính năng mới ExploreFragment
- [ ] Tạo `ExploreFragment.java` với danh sách người chơi gần
- [ ] Tạo `NearbyPlayerAdapter.java`
- [ ] Thêm GPS update vào User document
- [ ] Thêm nút "Tìm người chơi" cố định ở dưới cùng

### Sprint 3 — Radar Matchmaking
- [ ] Tạo `PlayerSession` model
- [ ] Tạo `RadarMatchActivity.java` + layout
- [ ] Tạo `RadarView.java` (custom animated view)
- [ ] Logic matchmaking trên Firestore
- [ ] Tạo `MatchFoundActivity.java` + layout
- [ ] Thêm Firestore security rules

### Sprint 4 — Polish & bug nhỏ
- [ ] **BUG-03:** Sửa rating hardcode trong CourtAdapter
- [ ] **BUG-05:** Tách ProfileFragment layout riêng
- [ ] **BUG-07:** Cleanup MainActivity
- [ ] Bottom nav badge cho pending bookings
- [ ] `avgRating` trong Court document

---

## Phụ lục: Dependencies có thể cần thêm

```gradle
// build.gradle (app level)
dependencies {
    // Firebase Location (đã có FusedLocation nếu đã dùng Maps)
    implementation 'com.google.android.gms:play-services-location:21.0.1'
    
    // Shimmer loading (optional)
    implementation 'com.facebook.shimmer:shimmer:0.5.0'
    
    // Lottie animation cho radar (optional, thay thế custom RadarView)
    implementation 'com.airbnb.android:lottie:6.0.0'
    
    // CountDownTimer đã có sẵn trong Android SDK, không cần thêm
}
```

---

## Ghi chú cho AI Coder

1. **Đừng thay đổi** các file model (`Booking`, `Court`, `User`) mà không kiểm tra tất cả nơi sử dụng — Firestore deserialization nhạy cảm với tên field.

2. **Khi sửa MapFragment**, test kỹ các trường hợp: có GPS + có sân, có GPS + không có sân, không có GPS, từ chối permission.

3. **PlayerSessions** cần có cleanup job (hoặc client-side check `expiresAt`) để tránh match với session đã hết hạn.

4. **RadarMatchActivity** phải hủy session khi user back hoặc app vào background (`onDestroy`, `onStop`).

5. **Firestore index** có thể cần tạo thêm cho query `PlayerSessions` theo `status` + `expiresAt`.

6. Toàn bộ **string tiếng Việt** mới cần thêm vào `res/values/strings.xml`.

7. **Màu sắc** đồng nhất: Radar background dùng `@color/text_primary` (#1D1D1F), sweep màu `@color/green_primary`, vòng tròn xanh nhạt `@color/green_light`.
