# PickleBook – Hướng dẫn cải tiến giao diện toàn diện

> Màu chủ đạo: **Xanh lá (#34C759 light / #32D74B dark)**  
> Phong cách: Apple-inspired, tối giản, card-based  
> Yêu cầu: Hỗ trợ cả Light Mode và Dark Mode  

---

## 1. HỆ THỐNG MÀU – CẦN ĐỒNG BỘ NGAY

### 1.1 File `values/colors.xml` – Bổ sung màu còn thiếu

Thêm vào cuối file (trước `</resources>`):

```xml
<!-- Booking / schedule specific – cần đồng bộ với schedule screen -->
<color name="booking_green_primary">#34C759</color>

<!-- Màu trạng thái sân (schedule grid) -->
<color name="slot_empty">#FFFFFF</color>
<color name="slot_booked">#FFCCCC</color>
<color name="slot_selected">#C8F0D4</color>
<color name="slot_locked">#D1D1D6</color>
<color name="booking_dark_green">#1B5E20</color>
<color name="booking_yellow">#E9A800</color>
```

> **Lý do**: Hiện tại `booking_yellow` được dùng ở `activity_booking_schedule.xml` và `activity_booking_confirm.xml` nhưng không có trong `colors.xml` chính → build error tiềm ẩn.

### 1.2 File `values-night/colors.xml` – Bổ sung màu thiếu cho dark mode

Thêm vào (trước `</resources>`):

```xml
<!-- Booking schedule colors – dark mode -->
<color name="slot_empty">#2C2C2E</color>
<color name="slot_booked">#5C2A2A</color>
<color name="slot_selected">#1C3A25</color>
<color name="slot_locked">#48484A</color>
<color name="booking_dark_green">#30D158</color>
<color name="booking_yellow">#FFD60A</color>

<!-- Bổ sung màu nền card cho dark mode -->
<color name="warning_text">#FFD60A</color>
<color name="warning_bg">#3A2E00</color>
<color name="white_60">#99FFFFFF</color>
<color name="white_80">#CCFFFFFF</color>
```

> **Lý do**: Các màu `slot_*`, `booking_*`, `warning_text`, `white_60`, `white_80` được dùng trong nhiều layout nhưng chỉ có ở `values/colors.xml`, không có ở `values-night/colors.xml` → Dark mode sẽ hiển thị sai màu.

---

## 2. ĐỒNG BỘ HỆ THỐNG ICON – LỖI NGHIÊM TRỌNG

### 2.1 Các icon cần chuẩn hóa

Hiện tại codebase dùng **lẫn lộn** nhiều loại icon:
- `@android:drawable/ic_media_previous` → nút Back (xấu, icon hệ thống cũ)
- `@drawable/ic_nav_court` → dùng làm icon địa chỉ (sai ngữ nghĩa, icon này là location pin)
- `@drawable/ic_nav_profile` → dùng làm icon điện thoại (sai ngữ nghĩa)
- `@android:drawable/ic_input_add` → FAB icon (icon hệ thống cũ)
- `@android:drawable/ic_menu_search` → icon tìm kiếm (icon hệ thống cũ)
- `@android:drawable/ic_menu_close_clear_cancel` → nút đóng (icon hệ thống cũ)

### 2.2 Tạo các file icon XML mới (trong `res/drawable/`)

**Tạo file `ic_back.xml`** (thay thế `@android:drawable/ic_media_previous`):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@android:color/black"
        android:pathData="M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z"/>
</vector>
```

**Tạo file `ic_phone.xml`** (thay thế `ic_nav_profile` dùng sai):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@android:color/black"
        android:pathData="M6.62,10.79c1.44,2.83 3.76,5.14 6.59,6.59l2.2,-2.2c0.27,-0.27 0.67,-0.36 1.02,-0.24 1.12,0.37 2.33,0.57 3.57,0.57 0.55,0 1,0.45 1,1V20c0,0.55 -0.45,1 -1,1 -9.39,0 -17,-7.61 -17,-17 0,-0.55 0.45,-1 1,-1h3.5c0.55,0 1,0.45 1,1 0,1.25 0.2,2.45 0.57,3.57 0.11,0.35 0.03,0.74 -0.24,1.02l-2.21,2.21z"/>
</vector>
```

**Tạo file `ic_location.xml`** (thay thế `ic_nav_court` dùng làm địa chỉ):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@android:color/black"
        android:pathData="M12,2C8.13,2 5,5.13 5,9c0,5.25 7,13 7,13s7,-7.75 7,-13c0,-3.87 -3.13,-7 -7,-7zM12,11.5c-1.38,0 -2.5,-1.12 -2.5,-2.5s1.12,-2.5 2.5,-2.5 2.5,1.12 2.5,2.5 -1.12,2.5 -2.5,2.5z"/>
</vector>
```

**Tạo file `ic_add.xml`** (thay thế `@android:drawable/ic_input_add`):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@android:color/black"
        android:pathData="M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
</vector>
```

**Tạo file `ic_search.xml`** (thay thế `@android:drawable/ic_menu_search`):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@android:color/black"
        android:pathData="M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z"/>
</vector>
```

**Tạo file `ic_close.xml`** (thay thế `@android:drawable/ic_menu_close_clear_cancel`):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@android:color/black"
        android:pathData="M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z"/>
</vector>
```

**Tạo file `ic_clock.xml`** (cho giờ mở cửa – hiện dùng `ic_nav_booking` sai ngữ nghĩa):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@android:color/black"
        android:pathData="M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8zM12.5,7H11v6l5.25,3.15 0.75,-1.23 -4.5,-2.67z"/>
</vector>
```

### 2.3 Thay thế icon trong TẤT CẢ layout files

**Quy tắc thay thế (tìm & thay toàn bộ project):**

| Tìm | Thay bằng | Ghi chú |
|-----|-----------|---------|
| `@android:drawable/ic_media_previous` | `@drawable/ic_back` | Tất cả nút Back |
| `@android:drawable/ic_input_add` | `@drawable/ic_add` | FAB và nút thêm |
| `@android:drawable/ic_menu_search` | `@drawable/ic_search` | Tất cả ô search |
| `@android:drawable/ic_menu_close_clear_cancel` | `@drawable/ic_close` | Nút đóng |
| `android:src="@drawable/ic_nav_court"` (khi dùng cho địa chỉ) | `@drawable/ic_location` | Chỉ khi context là địa chỉ |
| `android:src="@drawable/ic_nav_booking"` (khi dùng cho giờ) | `@drawable/ic_clock` | Chỉ khi context là giờ giấc |
| `android:src="@drawable/ic_nav_profile"` (khi dùng cho điện thoại) | `@drawable/ic_phone` | Chỉ khi context là SĐT |

---

## 3. TỪNG FILE LAYOUT – PHÂN TÍCH VÀ HƯỚNG SỬA

### 3.1 `activity_splash.xml`
**Vấn đề:**
- Nền `bg_white` cứng, dark mode sẽ vẫn trắng
- Text "PickleBall" và "Booking" dùng `textStyle="bold"` + `fontFamily="sans-serif-medium"` – redundant

**Hướng sửa:**
1. Đổi `android:background="@color/bg_white"` → `android:background="@color/bg_primary"` (tự thích ứng dark mode)
2. Card icon: thêm `app:strokeColor="@color/divider"` và `app:strokeWidth="0.5dp"` để có outline nhẹ trong dark mode
3. Xóa `android:textStyle="bold"` khỏi 2 TextView (giữ `fontFamily="sans-serif-medium"` là đủ)
4. ProgressBar: thêm `android:progressBackgroundTint="@color/divider"` để track background có màu

---

### 3.2 `activity_onboarding.xml` + `fragment_onboarding.xml`
**Vấn đề:**
- Nền `bg_white` cứng trong `activity_onboarding.xml` – dark mode sẽ sai
- Skip button: không có ripple feedback
- `fragment_onboarding.xml` nền `bg_white` cứng

**Hướng sửa:**
1. `activity_onboarding.xml`: `android:background="@color/bg_white"` → `@color/bg_primary`
2. `fragment_onboarding.xml`: `android:background="@color/bg_white"` → `@color/bg_primary`
3. Thêm `android:background="?attr/selectableItemBackgroundBorderless"` cho `tvSkip`
4. TabLayout dot indicator: thêm `android:layout_marginTop="8dp"` và `android:layout_marginBottom="16dp"` cho thoáng hơn
5. Nút "Tiếp theo": thêm `android:layout_marginTop="12dp"`

---

### 3.3 `activity_login.xml`
**Vấn đề:**
- `android:background="@color/bg_primary"` → OK
- CardView dùng `androidx.cardview.widget.CardView` thay vì `MaterialCardView` – không thống nhất
- Divider "hoac" dùng text thuần, không đẹp

**Hướng sửa:**
1. Đổi `androidx.cardview.widget.CardView` (bao quanh App Icon) → `com.google.android.material.card.MaterialCardView` (thêm `app:strokeColor="@color/divider"` + `app:strokeWidth="0.5dp"`)
2. Divider "hoac": bọc `TextView` "hoac" bằng `android:paddingHorizontal="16dp"` và thêm `android:textAllCaps="false"` + `android:letterSpacing="0.05"`
3. Google Sign-In button: đổi màu text sang `@color/text_primary` (hiện là `@color/green_primary` – đọc khó ở dark mode)
4. Thêm `android:scrollbars="none"` cho `ScrollView` ngoài cùng

---

### 3.4 `activity_register.xml`
**Vấn đề:**
- `Spinner` hiển thị theo style hệ thống, không đồng bộ với app
- `NestedScrollView` thiếu `android:fillViewport="true"` cho một số device

**Hướng sửa:**
1. Bao `Spinner` trong `TextInputLayout` style `@style/Widget.PickleBall.TextInput` hoặc dùng `AutoCompleteTextView` để đồng bộ style
2. Thêm `app:boxBackgroundColor="@color/input_bg"` cho tất cả `TextInputLayout` trong màn hình này
3. Nút "Tạo tài khoản": thêm `android:letterSpacing="0.01"` cho chuẩn hơn

---

### 3.5 `activity_forgot_password.xml`
**Vấn đề:**
- Icon dùng `@drawable/ic_launcher_foreground` làm icon khóa – sai ngữ nghĩa
- `android:tint` deprecated cho `ImageView`, dùng `app:tint` thay thế

**Hướng sửa:**
1. Tạo `ic_lock.xml` (icon khóa):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@android:color/black"
        android:pathData="M18,8h-1V6c0,-2.76 -2.24,-5 -5,-5S7,3.24 7,6v2H6c-1.1,0 -2,0.9 -2,2v10c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V10c0,-1.1 -0.9,-2 -2,-2zM12,17c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2 2,0.9 2,2 -0.9,2 -2,2zM15.1,8H8.9V6c0,-1.71 1.39,-3.1 3.1,-3.1 1.71,0 3.1,1.39 3.1,3.1v2z"/>
</vector>
```
2. Đổi `android:src="@drawable/ic_launcher_foreground"` → `android:src="@drawable/ic_lock"`
3. Đổi `android:tint="@color/green_dark"` → `app:tint="@color/green_primary"`
4. Thêm `android:scrollbars="none"` cho `ScrollView`

---

### 3.6 `activity_home.xml`
**Vấn đề:**
- Dùng `de.hdodenhof.circleimageview.CircleImageView` – phụ thuộc thư viện bên ngoài, cần kiểm tra dependency
- Filter buttons dùng `TextView` thuần với background drawable – không có ripple feedback
- `EditText` trong SearchBar không có `android:imeOptions="actionSearch"`
- Nút admin "☰" là emoji, không đồng nhất với phong cách icon app

**Hướng sửa:**
1. Thêm `android:background="?attr/selectableItemBackgroundBorderless"` cho `btnFilterNearMe`, `btnFilterAvailable`, `btnFilterIndoor`
2. Thêm `android:imeOptions="actionSearch"` và `android:inputType="text"` cho `edtSearch`
3. Thêm `android:singleLine="true"` cho `edtSearch`
4. `btnAdminMenu`: thay emoji "☰" bằng icon – tạo `ic_menu.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@android:color/black"
        android:pathData="M3,18h18v-2H3v2zm0,-5h18v-2H3v2zm0,-7v2h18V6H3z"/>
</vector>
```
Đổi thành `ImageView` với `android:src="@drawable/ic_menu"`

---

### 3.7 `activity_customer_home.xml`
**Vấn đề:**
- Header có `android:background="@color/bg_white"` → cứng, dark mode sai
- Hero Banner `android:background="@color/green_primary"` → OK nhưng cần xét dark mode
- Quick Action cards dùng emoji ✈️ 🏟️ 👤 trực tiếp trong `TextView` – không kiểm soát được size/color

**Hướng sửa:**
1. Header: `android:background="@color/bg_white"` → giữ nguyên nhưng bổ sung elevation shadow nhẹ: thêm `android:elevation="2dp"` hoặc dùng `MaterialCardView` bao ngoài với `app:cardElevation="0dp"` + `app:strokeColor="@color/divider"` + `app:strokeWidth="0.5dp"`
2. Hero Banner: thêm border radius dưới để có cảm giác card nổi:
   - Bọc `LinearLayout` hero bằng `com.google.android.material.card.MaterialCardView` với `app:cardCornerRadius="0dp"` (hoặc thêm bottom radius) và thêm hiệu ứng shadow nhẹ `app:cardElevation="4dp"`
3. Quick Action cards: thay emoji text bằng `ImageView` dùng icon drawable, rồi `TextView` tên bên dưới. Ví dụ card "Lịch đặt":
   - Xóa `<TextView android:text="&#128197;"/>`
   - Thêm: `<ImageView android:src="@drawable/ic_nav_booking" android:layout_width="32dp" android:layout_height="32dp" app:tint="@color/green_primary"/>`
4. "Lịch đặt sắp tới" card: thêm icon `ic_nav_booking` trước text `tvUpcomingCourtName`

---

### 3.8 `fragment_customer_home.xml`
**Vấn đề:**
- AppBar header `android:background="@color/green_primary"` – OK cho light, nhưng dark mode cần khác đi
- Icon `ic_nav_court` dùng làm logo trong SearchBar – sai ngữ nghĩa (nên là icon pickleball hoặc logo app)
- Search bar có quá nhiều icon: search + filter + heart → dày đặc, thiếu space

**Hướng sửa:**
1. AppBar dark mode: trong `values-night`, không cần đổi nhưng cần test contrast. Thêm `android:statusBarColor="@android:color/transparent"` nếu chưa có
2. Logo icon trong SearchBar: đổi `ic_nav_court` sang `ic_search` (hoặc tạo `ic_pickleball.xml` riêng)
3. Cân nhắc bỏ bớt icon heart trong SearchBar hoặc chuyển sang nút riêng bên ngoài. Giữ: search icon + filter icon. Bỏ heart icon ra ngoài card
4. Ngày tháng `tvDate`: thêm `android:textAllCaps="false"` (nếu thiếu)

---

### 3.9 `activity_court_detail.xml`
**Vấn đề:**
- AppBar height cứng `220dp` – trên màn hình nhỏ ảnh sẽ bị crop xấu
- `Toolbar` button Back dùng `@android:drawable/ic_media_previous` → cần đổi sang `ic_back`
- `tvRatingBadge` mặc định "Chưa có đánh giá" quá dài, dễ overflow trong badge nhỏ
- `TabLayout` dùng `app:tabMode="scrollable"` nhưng chỉ có ít tab → cân nhắc đổi sang `fixed`

**Hướng sửa:**
1. Đổi `android:layout_height="220dp"` của `AppBarLayout` → `200dp` để vừa hơn trên màn hình 5 inch
2. `btnBack`: đổi `android:src="@android:drawable/ic_media_previous"` → `android:src="@drawable/ic_back"`
3. `tvRatingBadge`: đổi text default thành "N/A" hoặc truncate bằng `android:maxLines="1"` + `android:ellipsize="end"` + `android:maxWidth="120dp"`
4. `TabLayout`: đổi `app:tabMode="scrollable"` → `app:tabMode="fixed"` nếu luôn có đúng 4-5 tab
5. Địa chỉ section: đổi `android:src="@drawable/ic_nav_court"` → `@drawable/ic_location` (cả 3 nơi dùng sai icon)
6. Giờ mở cửa section: đổi `android:src="@drawable/ic_nav_booking"` → `@drawable/ic_clock`
7. Số điện thoại section: đổi `android:src="@drawable/ic_nav_profile"` → `@drawable/ic_phone`

---

### 3.10 `activity_booking_schedule.xml`
**Vấn đề:**
- Header `android:background="@color/green_primary"` hardcode
- Nút Back: `@android:drawable/ic_media_previous` → cần đổi
- Legend items dùng `View` với background màu hardcode – dark mode sẽ không đổi
- Bottom panel `android:background="@color/text_primary"` → dark mode sẽ trông kỳ lạ (text_primary trở thành #FFFFFF trên nền trắng)
- `btnNext` dùng `app:backgroundTint="@color/booking_yellow"` – cần kiểm tra màu có đủ contrast với text trắng không (thực ra nên dùng text đen cho nút vàng)

**Hướng sửa:**
1. Nút Back: đổi sang `@drawable/ic_back`
2. Legend `View` elements: thêm `app:strokeColor="@color/divider"` và `app:strokeWidth="0.5dp"` cho View rỗng (slot trống) để visible ở dark mode. Thay `<View>` bằng `<View android:background="@drawable/bg_legend_empty"/>` etc.
3. Bottom panel: đổi `android:background="@color/text_primary"` → tạo một màu riêng `bottom_panel_bg`:
   - Thêm vào `values/colors.xml`: `<color name="bottom_panel_bg">#1D1D1F</color>`
   - Thêm vào `values-night/colors.xml`: `<color name="bottom_panel_bg">#000000</color>`
4. `btnNext` text: đổi `android:textColor="@color/white"` → `android:textColor="@color/text_primary"` (vì nền vàng cần text tối)
5. `tvTotalHours`, `tvTotalMoney`: hiện `android:textColor="@color/white"` – OK vì nền tối, giữ nguyên

---

### 3.11 `activity_booking_confirm.xml`
**Vấn đề:**
- Header `android:background="@color/green_primary"` – OK
- Nút Back: `@android:drawable/ic_media_previous` → đổi
- Tất cả info card dùng `app:cardBackgroundColor="@color/green_primary"` → text trắng trên nền xanh, dark mode cần test
- `btnConfirmBooking` dùng `android:backgroundTint="@color/booking_yellow"` với `android:textColor="@color/white"` → contrast ratio kém (vàng + trắng)

**Hướng sửa:**
1. Nút Back: `@drawable/ic_back`
2. `btnConfirmBooking`: đổi `android:textColor="@color/white"` → `android:textColor="@color/text_primary"` (đen trên vàng)
3. Info cards background: tạo màu semantic hơn – thêm `color name="card_accent_green"`:
   - `values/colors.xml`: `<color name="card_accent_green">#34C759</color>`
   - `values-night/colors.xml`: `<color name="card_accent_green">#1C3A25</color>` (tối hơn cho dark mode)
   - Đổi `app:cardBackgroundColor="@color/green_primary"` → `@color/card_accent_green`

---

### 3.12 `activity_booking_success.xml`
**Vấn đề:**
- `android:background="@color/bg_primary"` – OK
- `btnViewBookings`: `app:backgroundTint="@color/green_primary"` + `android:cornerRadius="26dp"` – OK nhưng chưa có ripple
- `btnGoHome`: dùng `@style/Widget.MaterialComponents.Button.OutlinedButton` thay vì `Widget.PickleBall.Button.Outline` – không nhất quán

**Hướng sửa:**
1. `btnGoHome`: đổi style → `style="@style/Widget.PickleBall.Button.Outline"` (bỏ các `app:strokeColor`, `app:cornerRadius` riêng lẻ vì style đã có)
2. Emoji ✅ trong `TextView`: đổi size `android:textSize="72sp"` → `64sp` (bớt chiếm chỗ trên màn nhỏ)
3. Thêm `android:clipToPadding="false"` cho container ngoài để button không bị clip

---

### 3.13 `activity_payment.xml`
**Vấn đề:**
- Header `android:background="@color/green_primary"` – OK
- Countdown warning card dùng hardcode `app:cardBackgroundColor="#FFF3CD"` và `android:textColor="#856404"` → dark mode không adapt
- `btnCancelPayment` dùng `@style/Widget.MaterialComponents.Button.OutlinedButton` – không nhất quán

**Hướng sửa:**
1. Countdown card: Đổi hardcode color → dùng `app:cardBackgroundColor="@color/warning_bg"` và `android:textColor="@color/warning_text"` (đã có trong `colors.xml`)
2. `btnCancelPayment`: đổi → `style="@style/Widget.PickleBall.Button.Outline"` + `app:strokeColor="@color/error_red"` + `android:textColor="@color/error_red"`
3. `layoutQrLoading` và `layoutQrContent`: thêm `android:minHeight="260dp"` cho `layoutQrContent` để không bị jump khi hiển thị

---

### 3.14 `activity_profile.xml`
**Vấn đề:**
- Header `android:background="@color/bg_white"` – dark mode sẽ sai
- Avatar card `app:strokeColor="@color/divider"` – OK nhưng `app:strokeWidth="2dp"` → hơi dày, đổi `1dp`
- Setting menu items dùng `android:text=">"` làm chevron icon – không nhất quán với rest of app

**Hướng sửa:**
1. Header: `android:background="@color/bg_white"` → giữ nhưng thêm bottom border nhẹ bằng `View` 0.5dp `@color/divider`
2. Avatar `app:strokeWidth="2dp"` → `1dp`
3. Tất cả `android:text=">"` trong settings menu → thay bằng `ImageView android:src="@drawable/ic_chevron_right"` với `android:layout_width="20dp"`, `android:layout_height="20dp"`, `app:tint="@color/text_tertiary"`
4. `tvChangeAvatar`: thêm `android:background="?attr/selectableItemBackgroundBorderless"`
5. `imgAvatar`: bao bằng `FrameLayout` và thêm overlay nửa trong suốt khi click (camera icon nhỏ ở góc dưới phải)

---

### 3.15 `activity_settings.xml`
**Vấn đề:**
- Tất cả `android:text=">"` làm chevron → không nhất quán (đã có `ic_chevron_right.xml` nhưng không dùng)
- `MaterialToolbar` `android:paddingTop="24dp"` – bị lệch trên các device có notch

**Hướng sửa:**
1. Thay tất cả `android:text=">"` → `ImageView android:src="@drawable/ic_chevron_right"` (tương tự profile)
2. `MaterialToolbar`: xóa `android:paddingTop="24dp"` – để system handle status bar insets
3. Thêm `android:fitsSystemWindows="true"` cho root `CoordinatorLayout`
4. Version text `android:text="Phiên bản 1.0"` → đổi thành dynamic từ code (BuildConfig.VERSION_NAME)

---

### 3.16 `activity_notifications.xml`
**Vấn đề:**
- Nút Back: `@android:drawable/ic_media_previous` → đổi
- `tvEmpty` không có padding đủ cho cảm giác centered

**Hướng sửa:**
1. Nút Back: `@drawable/ic_back`
2. `tvEmpty`: thêm `android:padding="40dp"` và `android:lineSpacingMultiplier="1.5"`

---

### 3.17 `activity_add_court.xml`
**Vấn đề:**
- Nút Back: `@android:drawable/ic_media_previous` → đổi
- `btnPickImage`: text dùng emoji trực tiếp "📷" – có thể hiển thị khác nhau trên các device
- `imgCourtPreview`: tint dùng `app:tint="@color/green_primary"` – deprecated từ API 23, dùng `android:tint` hoặc code

**Hướng sửa:**
1. Nút Back: `@drawable/ic_back`
2. `btnPickImage` text: Đổi "📷  Chọn ảnh sân" → "Chọn ảnh sân" và thêm `app:icon="@drawable/ic_camera"` (tạo file mới):
```xml
<!-- ic_camera.xml -->
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@android:color/black"
        android:pathData="M12,12m-3.2,0a3.2,3.2 0,1 1,6.4 0a3.2,3.2 0,1 1,-6.4 0"/>
    <path android:fillColor="@android:color/black"
        android:pathData="M9,2L7.17,4H4C2.9,4 2,4.9 2,6v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V6c0,-1.1 -0.9,-2 -2,-2h-3.17L15,2H9zM12,17c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5z"/>
</vector>
```
3. `imgCourtPreview` tint: xóa `app:tint` → xử lý tint trong code với `ImageViewCompat.setImageTintList()`

---

### 3.18 `activity_owner_home.xml` + `fragment_owner_dashboard.xml`
**Vấn đề:**
- Hai file này gần như **duplicate hoàn toàn** → rất khó maintain
- Header dùng `android:background="@color/bg_white"` – dark mode sẽ sai

**Hướng sửa:**
1. **Quan trọng**: Chọn 1 trong 2 file để sử dụng. Nếu dùng fragment-based navigation (owner_main.xml có FrameLayout), thì chỉ cần `fragment_owner_dashboard.xml`. File `activity_owner_home.xml` có thể bỏ hoặc convert sang dùng fragment
2. Tất cả `android:text="›"` làm chevron → đổi thành `ImageView` `ic_chevron_right`
3. Header background: wrap trong `MaterialCardView` với shadow nhẹ để tạo elevation effect thay vì flat white

---

### 3.19 `activity_admin_home.xml` + `fragment_admin_dashboard.xml`
**Vấn đề:**
- Tương tự owner: **duplicate** nội dung giữa activity và fragment
- Header `android:background="@color/text_primary"` – ổn cho dark header nhưng dark mode sẽ cần kiểm tra

**Hướng sửa:**
1. Chọn 1 trong 2 để dùng (ưu tiên fragment)
2. Tất cả `android:text="›"` → đổi thành `ImageView` `ic_chevron_right` với `app:tint="@color/text_tertiary"`
3. ADMIN badge: thêm `android:letterSpacing="0.05"` cho text "ADMIN"

---

### 3.20 `fragment_admin_courts.xml`
**Vấn đề:**
- Header background `android:background="@color/text_primary"` – dark mode inverts này thành trắng (xem `values-night`: `text_primary = #FFFFFF`) → header sẽ thành trắng trong dark mode!

**Hướng sửa – QUAN TRỌNG:**
1. Tạo màu semantic riêng cho dark header:
   - `values/colors.xml`: `<color name="header_dark_bg">#1D1D1F</color>`
   - `values-night/colors.xml`: `<color name="header_dark_bg">#000000</color>`
2. Đổi `android:background="@color/text_primary"` → `android:background="@color/header_dark_bg"` trong TẤT CẢ các màn hình dùng dark header:
   - `fragment_admin_courts.xml`
   - `fragment_admin_dashboard.xml`
   - `fragment_admin_users.xml`
   - `activity_radar_match.xml` (background root ConstraintLayout)

---

### 3.21 `fragment_admin_users.xml`
**Vấn đề:**
- Header background `android:background="@color/text_primary"` → xem lỗi ở 3.20

**Hướng sửa:**
1. Đổi sang `@color/header_dark_bg` (tạo ở 3.20)

---

### 3.22 `activity_radar_match.xml`
**Vấn đề:**
- Root background `android:background="@color/text_primary"` → dark mode lỗi (xem 3.20)
- Nút Back dùng `ImageButton` thay vì `ImageView` – inconsistent với app
- `app:tint="@color/white"` cho icon → `white` trong dark mode là `#FF000000` (đen!) – lỗi nghiêm trọng

**Hướng sửa:**
1. Root background: đổi → `@color/header_dark_bg`
2. Tất cả `app:tint="@color/white"` → `app:tint="@android:color/white"` (hardcode white, không bị invert)
3. `btnCancel` background `app:backgroundTint="#44FFFFFF"` → đổi thành `@color/white_20`:
   - Thêm vào `values/colors.xml`: `<color name="white_20">#33FFFFFF</color>`
   - Thêm vào `values-night/colors.xml`: `<color name="white_20">#33FFFFFF</color>` (giữ nguyên)
4. `btnBack` `ImageButton` → đổi thành `ImageView` (consistent với app):
   ```xml
   <ImageView android:id="@+id/btnBack" ... android:src="@drawable/ic_back" app:tint="@android:color/white"/>
   ```

---

### 3.23 `activity_match_found.xml`
**Vấn đề:**
- Header `android:background="@color/green_primary"` – OK
- `btnSearchAgain` dùng `@style/Widget.MaterialComponents.Button.OutlinedButton` → đổi sang style app
- `CircleImageView` cần dependency check

**Hướng sửa:**
1. `btnSearchAgain`: đổi → `style="@style/Widget.PickleBall.Button.Outline"`
2. Xóa các `app:strokeColor`, `app:cornerRadius` riêng lẻ (style đã có)
3. Countdown text: thêm `android:fontFamily="sans-serif-medium"` cho `tvCountdown`

---

### 3.24 `fragment_explore.xml`
**Vấn đề:**
- AppBar `android:background="@color/green_primary"` – OK
- `btnFindPlayer` nút CỐ ĐỊNH ở dưới dùng emoji "🏓" trong text → inconsistent
- `btnEnableGps` không có explicit style

**Hướng sửa:**
1. `btnFindPlayer` text: đổi "🏓  Tìm người chơi" → "Tìm người chơi" và thêm `app:icon="@drawable/ic_nav_explore"` (hoặc icon paddle)
2. `btnEnableGps`: thêm `style="@style/Widget.PickleBall.Button.Primary"` và `android:layout_height="44dp"`

---

### 3.25 `fragment_customer_map.xml`
**Vấn đề:**
- Search card đặt trên bản đồ dùng hardcode elevation `app:cardElevation="6dp"` – inconsistent (system mặc định là 4dp)
- `bottomSheet` dùng `MaterialCardView` làm BottomSheet container – có thể gây issue với `BottomSheetBehavior` (nên dùng `FrameLayout` hoặc `LinearLayout`)

**Hướng sửa:**
1. Search card: đổi `app:cardElevation="6dp"` → `4dp`
2. Bottom Sheet: giữ nguyên nếu hoạt động OK, nhưng đổi corner radius: `app:cardCornerRadius="18dp"` → `20dp` cho consistent với design system
3. `btnOpenCourtDetail`: đổi `style="@style/Widget.PickleBall.Button.Primary"` và bỏ các override riêng

---

### 3.26 `fragment_my_bookings.xml` + `fragment_owner_bookings.xml`
**Vấn đề:**
- Header `android:background="@color/bg_white"` – dark mode sẽ sai (context: header trắng với text tối)
- Filter tabs dùng mix style: tab active dùng `bg_button_primary`, tab inactive dùng `bg_search_bar` → inconsistent background radius

**Hướng sửa:**
1. Header: thêm bottom border View `0.5dp` màu `@color/divider` phía dưới header
2. Filter tabs: Chuẩn hóa – tất cả dùng `MaterialButton` với toggle state hoặc dùng `ChipGroup` + `Chip` (đồng bộ hơn với Material Design)

---

### 3.27 `activity_owner_booking_manage.xml`
**Vấn đề:**
- Tương tự fragment_owner_bookings
- `tabAll`, `tabPending`, `tabConfirmed` là `TextView` không có state properly

**Hướng sửa:**
- Đổi sang `ChipGroup` + `Chip` với `app:chipBackgroundColor` selector

---

### 3.28 `fragment_tab_reviews.xml`
**Vấn đề:**
- `btnWriteReview` dùng emoji "✏️" trong text
- Rating bars dùng `android.widget.ProgressBar` không có explicit style → trông khác nhau trên các Android version

**Hướng sửa:**
1. `btnWriteReview`: đổi text → "Viết đánh giá" và `app:icon="@drawable/ic_edit"` (tạo icon edit mới)
2. ProgressBar: thêm `android:progressBackgroundTint="@color/divider"` + `android:progressTint="@color/green_primary"` cho tất cả `bar1`-`bar5`

---

### 3.29 Item layouts

**`item_court.xml`**
- `btnBookItem` dùng `android:textAllCaps="true"` → inconsistent với style Primary đã set `false`. **Xóa `android:textAllCaps="true"`**.
- Emoji "⭐" trong `tvRating` → thay bằng `ImageView` star icon + `TextView` text
- Avatar card với `de.hdodenhof.circleimageview` → OK nếu có dependency

**`item_booking.xml`** + **`item_booking_manage.xml`**
- Text status dùng emoji "⏳" "✅" "❌" trong `TextView` → không kiểm soát được size, màu ở dark mode
- **Hướng sửa**: Tạo status badge riêng bằng `MaterialCardView` nhỏ với background color theo trạng thái và text không emoji

**`item_court_admin.xml`** + **`item_court_owner.xml`**
- `btnAdminDeactivate` dùng `app:strokeColor="@color/error_red"` override style → OK
- `btnEditCourt` text dùng emoji "✏️" → đổi sang icon

**`item_nearby_player.xml`**
- `btnChallenge` dùng `@style/Widget.MaterialComponents.Button.OutlinedButton` → đổi sang `Widget.PickleBall.Button.Outline`

**`item_user.xml`**
- `FrameLayout` avatar dùng hardcode `android:background="@color/green_primary"` → OK nhưng cần `android:background="@drawable/bg_avatar_circle"` để có dạng tròn (hiện là hình vuông!)
- `tvAvatarChar` cần `android:layout_gravity="center"` đã có → OK

**`item_notification.xml`**
- Thiếu separator View giữa các item. Thêm `<View android:layout_height="0.5dp" android:background="@color/divider"/>` ở cuối layout

---

## 4. DRAWABLES – CẦN BỔ SUNG VÀ SỬA

### 4.1 `bg_badge_inactive.xml`
Hardcode `#888888` → đổi sang `@color/text_tertiary` (tự adapt dark mode):
```xml
<solid android:color="@color/text_tertiary"/>
```

### 4.2 `bg_header_gradient.xml`
Gradient `startColor="@color/green_tint"` → `centerColor="@color/green_primary"` → `endColor="@color/green_dark"` – OK về màu sắc.
Dark mode: trong `values-night`, `green_tint = #1A2E1F`, `green_primary = #32D74B` → gradient sẽ khác. Cần tạo `drawable-night/bg_header_gradient.xml` với màu phù hợp.

### 4.3 `bg_rounded_top.xml`
Hardcode `@color/white` → đổi → `@color/bg_white`:
```xml
<solid android:color="@color/bg_white"/>
```

### 4.4 `bg_search_bar.xml`
Hardcode `@color/white` → đổi → `@color/bg_white`:
```xml
<solid android:color="@color/bg_white"/>
```

### 4.5 Tạo `drawable-night/` cho các drawable có màu cứng

Tạo folder `res/drawable-night/` và copy các file sau, đổi màu cho dark mode:
- `bg_card.xml`: đổi `@color/bg_card` → giữ (đã semantic)
- `bg_input.xml`: đổi `@color/input_bg` → giữ (đã semantic)
- `bg_rounded_top.xml`: đổi `@color/white` → `@color/bg_white`
- `bg_search_bar.xml`: đổi `@color/white` → `@color/bg_white`
- `bg_slot_booked.xml`: đổi hardcode `#FFCCCC` → `<solid android:color="@color/slot_booked"/>`

---

## 5. THEMES – CẦN SỬA

### 5.1 `values/themes.xml`
**Vấn đề**: `Base.Theme.PickleBallBookingApp` với `parent="Theme.Material3.DayNight.NoActionBar"` được define nhưng không sử dụng – gây confuse. Xóa hoặc comment out toàn bộ `Base.Theme.PickleBallBookingApp` style (và các comment bị duplicate bên dưới).

**Hướng sửa**: Xóa 4 dòng:
```xml
<!-- XÓA: -->
<style name="Base.Theme.PickleBallBookingApp" parent="Theme.Material3.DayNight.NoActionBar"></style>
<!-- Và tất cả comment duplicate bên dưới -->
```

### 5.2 Thêm style cho Chip
Thêm vào `values/themes.xml`:
```xml
<style name="Widget.PickleBall.Chip" parent="Widget.MaterialComponents.Chip.Action">
    <item name="chipBackgroundColor">@color/chip_bg_selector</item>
    <item name="chipStrokeColor">@color/green_primary</item>
    <item name="chipStrokeWidth">1dp</item>
    <item name="android:textColor">@color/chip_text_selector</item>
    <item name="chipCornerRadius">16dp</item>
</style>
```

Tạo `res/color/chip_bg_selector.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="@color/green_primary" android:state_checked="true"/>
    <item android:color="@color/bg_white"/>
</selector>
```

Tạo `res/color/chip_text_selector.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="@color/white" android:state_checked="true"/>
    <item android:color="@color/text_secondary"/>
</selector>
```

---

## 6. CHECKLIST TỔNG HỢP – CÁC LỖI CẦN FIX THEO PRIORITY

### 🔴 Priority 1 – Fix ngay (build/display error)

1. [ ] Thêm `slot_*`, `booking_*` colors vào `values-night/colors.xml`
2. [ ] Fix `android:background="@color/text_primary"` trong `fragment_admin_courts.xml`, `fragment_admin_dashboard.xml`, `fragment_admin_users.xml`, `activity_radar_match.xml` → đổi sang `header_dark_bg`
3. [ ] Fix `app:tint="@color/white"` trong `activity_radar_match.xml` → đổi sang `@android:color/white`
4. [ ] Fix `bg_rounded_top.xml` và `bg_search_bar.xml` dùng `@color/white` → `@color/bg_white`
5. [ ] Fix `bg_slot_booked.xml` hardcode `#FFCCCC` → `@color/slot_booked`
6. [ ] Xóa duplicate `Base.Theme.PickleBallBookingApp` styles trong `themes.xml`

### 🟠 Priority 2 – Icon consistency (UX)

7. [ ] Tạo tất cả icon mới: `ic_back`, `ic_phone`, `ic_location`, `ic_add`, `ic_search`, `ic_close`, `ic_clock`, `ic_lock`, `ic_camera`, `ic_menu`, `ic_edit`
8. [ ] Thay toàn bộ `@android:drawable/ic_media_previous` → `@drawable/ic_back` (12 chỗ)
9. [ ] Thay toàn bộ `@android:drawable/ic_input_add` → `@drawable/ic_add` (2 chỗ)
10. [ ] Thay toàn bộ `@android:drawable/ic_menu_search` → `@drawable/ic_search` (4 chỗ)
11. [ ] Thay `@android:drawable/ic_menu_close_clear_cancel` → `@drawable/ic_close`
12. [ ] Fix icon dùng sai ngữ nghĩa (ic_nav_court cho địa chỉ → ic_location, ic_nav_booking cho giờ → ic_clock, ic_nav_profile cho điện thoại → ic_phone)

### 🟡 Priority 3 – Visual polish (UI quality)

13. [ ] Thay tất cả `android:text=">"` và `android:text="›"` → `ImageView ic_chevron_right`
14. [ ] Fix chevron trong `activity_profile.xml` và `activity_settings.xml` (10+ chỗ)
15. [ ] Fix `item_user.xml` avatar từ `FrameLayout` square → circular bằng `bg_avatar_circle`
16. [ ] Fix `btnConfirmBooking` trong booking_confirm text color (trắng trên vàng → đen trên vàng)
17. [ ] Fix `btnNext` trong booking_schedule text color (trắng trên vàng → đen trên vàng)
18. [ ] Đổi `androidx.cardview.widget.CardView` → `MaterialCardView` trong `activity_login.xml`
19. [ ] Fix `bg_badge_inactive.xml` hardcode `#888888` → `@color/text_tertiary`
20. [ ] Thêm `android:scrollbars="none"` cho tất cả `ScrollView` trong auth screens

### 🟢 Priority 4 – Consistency & polish

21. [ ] Đổi tất cả `btnSearchAgain`, `btnGoHome` style từ `Widget.MaterialComponents.Button.OutlinedButton` → `Widget.PickleBall.Button.Outline`
22. [ ] Xóa `android:textStyle="bold"` khi đã có `fontFamily="sans-serif-medium"`
23. [ ] Thêm `android:background="?attr/selectableItemBackgroundBorderless"` cho các tap target chưa có
24. [ ] Chuẩn hóa filter tabs về dùng `ChipGroup` + `Chip` hoặc ít nhất dùng cùng style
25. [ ] Thêm separator giữa notification items trong `item_notification.xml`
26. [ ] Fix `activity_owner_home.xml` – nếu không dùng, xóa để giảm confusion

---

## 7. GHI CHÚ QUAN TRỌNG CHO AI CODER

1. **Không tự ý thay đổi ID** của các View (`android:id`) – có thể ảnh hưởng đến code Java/Kotlin
2. **Giữ nguyên tên file** drawable khi sửa nội dung, chỉ tạo file mới khi icon không tồn tại
3. **Test tất cả màn hình ở cả light mode và dark mode** sau khi fix
4. Khi sửa màu hardcode sang semantic color, **luôn kiểm tra** `values-night/colors.xml` có màu tương ứng không
5. **Không merge** `activity_owner_home.xml` với `fragment_owner_dashboard.xml` nếu không hiểu navigation flow – hỏi developer trước
6. `booking_yellow = #E9A800` có độ sáng thấp → **text trên nút màu này phải là `@color/text_primary` (đen)**, không phải trắng
7. Khi thêm `app:tint` cho `ImageView`, kiểm tra xem icon có `fillColor="@android:color/black"` chưa – nếu có thì tint sẽ hoạt động đúng; nếu icon có màu hardcode khác thì tint có thể không work

---

## 8. FILES CẦN TẠO MỚI (TỔNG HỢP)

### Drawable files mới:
- `res/drawable/ic_back.xml`
- `res/drawable/ic_phone.xml`
- `res/drawable/ic_location.xml`
- `res/drawable/ic_add.xml`
- `res/drawable/ic_search.xml`
- `res/drawable/ic_close.xml`
- `res/drawable/ic_clock.xml`
- `res/drawable/ic_lock.xml`
- `res/drawable/ic_camera.xml`
- `res/drawable/ic_menu.xml`
- `res/drawable/ic_edit.xml`

### Color selector files mới:
- `res/color/chip_bg_selector.xml`
- `res/color/chip_text_selector.xml`

### Drawable night variants:
- `res/drawable-night/bg_rounded_top.xml`
- `res/drawable-night/bg_search_bar.xml`

---

*Tài liệu này bao gồm phân tích 120+ file XML. Ưu tiên fix Priority 1 và 2 trước khi nộp.*
