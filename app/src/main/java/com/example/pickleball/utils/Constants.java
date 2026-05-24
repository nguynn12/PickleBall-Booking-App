package com.example.pickleball.utils;

/**
 * App-wide constants for PickleBall
 * Centralized to avoid magic strings
 */
public class Constants {

    // ==================== USER ROLES ====================
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_OWNER = "owner";
    public static final String ROLE_CUSTOMER = "user";

    // ==================== SKILL LEVELS ====================
    public static final String SKILL_BEGINNER = "beginner";
    public static final String SKILL_INTERMEDIATE = "intermediate";
    public static final String SKILL_PRO = "pro";

    // ==================== FIREBASE COLLECTIONS ====================
    public static final String COLLECTION_USERS = "Users";
    public static final String COLLECTION_COURTS = "Courts";
    public static final String COLLECTION_BOOKINGS = "Bookings";
    public static final String COLLECTION_REVIEWS = "Reviews";
    public static final String COLLECTION_FAVORITES = "Favorites";

    // ==================== FIREBASE FIELDS ====================
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_FULL_NAME = "fullName";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_PHONE = "phone";
    public static final String FIELD_ROLE = "role";
    public static final String FIELD_SKILL_LEVEL = "skillLevel";
    public static final String FIELD_AVATAR_URL = "avatarUrl";
    public static final String FIELD_CREATED_AT = "createdAt";

    // ==================== SHARED PREFERENCES ====================
    public static final String PREFS_NAME = "PickleBallPrefs";
    public static final String PREFS_ONBOARDING_DONE = "onboardingDone";
    public static final String PREFS_REMEMBER_ME = "rememberMe";

    // ==================== FIREBASE STORAGE PATHS ====================
    public static final String STORAGE_AVATARS = "avatars/";
    public static final String STORAGE_COURT_IMAGES = "court_images/";

    // ==================== VALIDATION LIMITS ====================
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MIN_PHONE_LENGTH = 10;
    public static final int MAX_PHONE_LENGTH = 11;
    public static final int MIN_NAME_LENGTH = 2;

    // ==================== INTENTS & EXTRAS ====================
    public static final String EXTRA_USER = "extra_user";
    public static final String EXTRA_COURT = "extra_court";
    public static final String EXTRA_BOOKING = "extra_booking";

    // ==================== ERROR MESSAGES ====================
    public static final String ERROR_EMPTY_FIELDS = "Vui lòng nhập đủ thông tin!";
    public static final String ERROR_INVALID_EMAIL = "Email không hợp lệ!";
    public static final String ERROR_INVALID_PHONE = "Số điện thoại không hợp lệ!";
    public static final String ERROR_PASSWORD_TOO_SHORT = "Mật khẩu phải có ít nhất 6 ký tự!";
    public static final String ERROR_PASSWORD_NOT_MATCH = "Mật khẩu không khớp!";
    public static final String ERROR_WRONG_CREDENTIALS = "Sai email hoặc mật khẩu!";
    public static final String ERROR_NETWORK = "Lỗi kết nối mạng!";
    public static final String ERROR_UNKNOWN = "Đã xảy ra lỗi không xác định!";

    // ==================== SUCCESS MESSAGES ====================
    public static final String SUCCESS_REGISTER = "Đăng ký thành công!";
    public static final String SUCCESS_LOGIN = "Đăng nhập thành công!";
    public static final String SUCCESS_LOGOUT = "Đăng xuất thành công!";
    public static final String SUCCESS_UPDATE = "Cập nhật thành công!";
    public static final String SUCCESS_UPLOAD = "Tải lên thành công!";

    // ==================== IMAGE PICKER ====================
    public static final int MAX_IMAGE_SIZE_MB = 5;
    public static final int COMPRESSED_IMAGE_QUALITY = 80;
    public static final int MAX_IMAGE_DIMENSION = 1024;

    // ==================== PAYMENT / DEPOSIT ====================
    public static final double DEPOSIT_RATE            = 0.30;                  // 30% tổng tiền
    public static final long   PAYMENT_TIMEOUT_MS      = 15 * 60 * 1000L;      // 15 phút QR
    public static final long   FREE_CANCEL_WINDOW_MS   = 2 * 60 * 60 * 1000L; // 2h trước giờ chơi

    // ==================== BOOKING STATUSES ====================
    public static final String BOOKING_STATUS_AWAITING_PAYMENT   = "awaiting_payment";
    public static final String BOOKING_STATUS_CONFIRMED          = "confirmed";
    public static final String BOOKING_STATUS_CANCELLED_BY_USER  = "cancelled_by_user";
    public static final String BOOKING_STATUS_CANCELLED_BY_OWNER = "cancelled_by_owner";
    public static final String BOOKING_STATUS_CANCELLED_NO_SHOW  = "cancelled_no_show";
    public static final String BOOKING_STATUS_CANCELLED_TIMEOUT  = "cancelled_timeout";
    public static final String BOOKING_STATUS_CANCELLED_CONFLICT = "cancelled_conflict";
    public static final String BOOKING_STATUS_COMPLETED          = "completed";

    // ==================== REFUND STATUSES ====================
    public static final String REFUND_STATUS_PENDING  = "refund_pending";
    public static final String REFUND_STATUS_DONE     = "refund_done";
    public static final String REFUND_STATUS_REJECTED = "refund_rejected";
    public static final String REFUND_STATUS_NA       = "not_applicable";

    // ==================== PAYMENT STATUSES ====================
    public static final String PAYMENT_STATUS_PENDING  = "pending";
    public static final String PAYMENT_STATUS_PAID     = "paid";
    public static final String PAYMENT_STATUS_REFUNDED = "refunded";
    public static final String PAYMENT_STATUS_EXPIRED  = "expired";

    // ==================== CLOUD FUNCTION URL ====================
    // Thay YOUR_PROJECT bằng Firebase project ID thực tế
    public static final String CLOUD_FN_CREATE_PAYMENT =
            "https://asia-southeast1-YOUR_PROJECT.cloudfunctions.net/createPaymentLink";

    // Private constructor to prevent instantiation
    private Constants() {
        throw new AssertionError("Cannot instantiate Constants class");
    }
}
