package com.example.pickleball.activity.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.pickleball.R;
import com.example.pickleball.activity.auth.LoginActivity;
import com.example.pickleball.utils.Constants;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    // Views
    private MaterialToolbar toolbar;
    private LinearLayout btnAccount, btnNotifications, btnLanguage, btnDarkMode,
                        btnAbout, btnPrivacy, btnTerms, btnLogout;
    private SwitchMaterial switchNotifications, switchDarkMode;
    private TextView tvNotificationStatus, tvLanguageValue, tvDarkModeStatus, tvAppVersion;

    // SharedPreferences
    private SharedPreferences prefs;
    private static final String PREFS_SETTINGS = "PickleBallSettings";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_DARK_MODE = "dark_mode";

    // Language options
    private final String[] LANGUAGES = {"Tiếng Việt", "English"};
    private final String[] LANGUAGE_CODES = {"vi", "en"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize SharedPreferences
        prefs = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Load current settings
        loadSettings();

        // Setup click listeners
        setupClickListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);

        // Account section
        btnAccount = findViewById(R.id.btnAccount);

        // Notifications
        btnNotifications = findViewById(R.id.btnNotifications);
        switchNotifications = findViewById(R.id.switchNotifications);
        tvNotificationStatus = findViewById(R.id.tvNotificationStatus);

        // Language - đã bỏ khỏi layout, set null để tránh crash
        btnLanguage = null;
        tvLanguageValue = null;

        // Dark Mode
        btnDarkMode = findViewById(R.id.btnDarkMode);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        tvDarkModeStatus = findViewById(R.id.tvDarkModeStatus);

        // About & Legal
        btnAbout = findViewById(R.id.btnAbout);
        btnPrivacy = findViewById(R.id.btnPrivacy);
        btnTerms = findViewById(R.id.btnTerms);
        tvAppVersion = findViewById(R.id.tvAppVersion);

        // Logout
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings_title);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadSettings() {
        // Load notifications setting
        boolean notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true);
        switchNotifications.setChecked(notificationsEnabled);
        updateNotificationStatus(notificationsEnabled);

        // Load language setting - đã bỏ
        // String language = prefs.getString(KEY_LANGUAGE, "vi");
        // updateLanguageDisplay(language);

        // Load dark mode setting
        boolean darkModeEnabled = prefs.getBoolean(KEY_DARK_MODE, false);
        switchDarkMode.setChecked(darkModeEnabled);
        updateDarkModeStatus(darkModeEnabled);

        // Set app version
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvAppVersion.setText("Phiên bản " + versionName);
        } catch (Exception e) {
            tvAppVersion.setText("Phiên bản 1.0");
        }
    }

    private void setupClickListeners() {
        // Account
        btnAccount.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });

        // Notifications toggle
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) { // Only handle user clicks, not programmatic
                saveNotificationSetting(isChecked);
                updateNotificationStatus(isChecked);
            }
        });

        btnNotifications.setOnClickListener(v -> {
            switchNotifications.setChecked(!switchNotifications.isChecked());
        });

        // Language selection - đã bỏ
        // btnLanguage.setOnClickListener(v -> showLanguageDialog());

        // Dark Mode toggle
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                saveDarkModeSetting(isChecked);
                updateDarkModeStatus(isChecked);
                applyDarkMode(isChecked);
            }
        });

        btnDarkMode.setOnClickListener(v -> {
            switchDarkMode.setChecked(!switchDarkMode.isChecked());
        });

        // About
        btnAbout.setOnClickListener(v -> showAboutDialog());

        // Privacy Policy
        btnPrivacy.setOnClickListener(v -> showPrivacyPolicy());

        // Terms of Service
        btnTerms.setOnClickListener(v -> showTermsOfService());

        // Logout
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());
    }

    // ─── NOTIFICATIONS ───────────────────────────────────────────────────────

    private void saveNotificationSetting(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply();

        // TODO: Register/Unregister FCM token here
        if (enabled) {
            // Enable push notifications
        } else {
            // Disable push notifications
        }
    }

    private void updateNotificationStatus(boolean enabled) {
        if (enabled) {
            tvNotificationStatus.setText("Bật");
            tvNotificationStatus.setTextColor(getColor(R.color.green_dark));
        } else {
            tvNotificationStatus.setText("Tắt");
            tvNotificationStatus.setTextColor(getColor(R.color.text_tertiary));
        }
    }

    // ─── LANGUAGE ────────────────────────────────────────────────────────────

    private void showLanguageDialog() {
        String currentLanguage = prefs.getString(KEY_LANGUAGE, "vi");
        int selectedIndex = currentLanguage.equals("vi") ? 0 : 1;

        new AlertDialog.Builder(this)
            .setTitle("Chọn ngôn ngữ")
            .setSingleChoiceItems(LANGUAGES, selectedIndex, (dialog, which) -> {
                String newLanguage = LANGUAGE_CODES[which];
                saveLanguageSetting(newLanguage);
                updateLanguageDisplay(newLanguage);
                dialog.dismiss();

                // Show restart prompt
                showRestartPrompt();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void saveLanguageSetting(String languageCode) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
    }

    private void updateLanguageDisplay(String languageCode) {
        if ("vi".equals(languageCode)) {
            tvLanguageValue.setText("Tiếng Việt");
        } else {
            tvLanguageValue.setText("English");
        }
    }

    private void showRestartPrompt() {
        new AlertDialog.Builder(this)
            .setTitle("Khởi động lại")
            .setMessage("Ứng dụng cần khởi động lại để áp dụng ngôn ngữ mới. Bạn có muốn khởi động lại ngay không?")
            .setPositiveButton(R.string.yes, (dialog, which) -> {
                // Restart app
                Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            })
            .setNegativeButton("Để sau", null)
            .show();
    }

    // ─── DARK MODE ───────────────────────────────────────────────────────────

    private void saveDarkModeSetting(boolean enabled) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    private void updateDarkModeStatus(boolean enabled) {
        if (enabled) {
            tvDarkModeStatus.setText("Bật");
            tvDarkModeStatus.setTextColor(getColor(R.color.green_dark));
        } else {
            tvDarkModeStatus.setText("Tắt");
            tvDarkModeStatus.setTextColor(getColor(R.color.text_tertiary));
        }
    }

    private void applyDarkMode(boolean enabled) {
        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    // ─── ABOUT & LEGAL ───────────────────────────────────────────────────────

    private void showAboutDialog() {
        String message = "PickleBook - Ứng dụng đặt sân Pickleball\n\n" +
                        "Phiên bản: " + tvAppVersion.getText() + "\n\n" +
                        "Được phát triển bởi PickleBall Team\n" +
                        "© 2025 PickleBook. All rights reserved.\n\n" +
                        "Ứng dụng giúp bạn dễ dàng tìm kiếm và đặt sân pickleball gần nhất.";

        new AlertDialog.Builder(this)
            .setTitle(R.string.settings_about)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show();
    }

    private void showPrivacyPolicy() {
        String policy = "CHÍNH SÁCH BẢO MẬT\n\n" +
                       "1. Thu thập thông tin\n" +
                       "Chúng tôi thu thập thông tin cá nhân bạn cung cấp khi đăng ký tài khoản:\n" +
                       "- Họ tên\n" +
                       "- Email\n" +
                       "- Số điện thoại\n" +
                       "- Ảnh đại diện\n\n" +
                       "2. Sử dụng thông tin\n" +
                       "Thông tin của bạn được sử dụng để:\n" +
                       "- Quản lý tài khoản\n" +
                       "- Xử lý đặt sân\n" +
                       "- Gửi thông báo quan trọng\n\n" +
                       "3. Bảo mật\n" +
                       "Chúng tôi sử dụng Firebase để bảo vệ dữ liệu của bạn.\n\n" +
                       "4. Quyền của bạn\n" +
                       "Bạn có quyền xem, sửa, xóa thông tin cá nhân bất cứ lúc nào.";

        new AlertDialog.Builder(this)
            .setTitle(R.string.settings_privacy)
            .setMessage(policy)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton("Tôi đồng ý", null)
            .show();
    }

    private void showTermsOfService() {
        String terms = "ĐIỀU KHOẢN SỬ DỤNG\n\n" +
                      "1. Chấp nhận điều khoản\n" +
                      "Bằng việc sử dụng PickleBook, bạn đồng ý với các điều khoản này.\n\n" +
                      "2. Tài khoản\n" +
                      "- Bạn chịu trách nhiệm bảo mật tài khoản\n" +
                      "- Không chia sẻ mật khẩu cho người khác\n" +
                      "- Cung cấp thông tin chính xác\n\n" +
                      "3. Đặt sân\n" +
                      "- Thanh toán đầy đủ trước khi sử dụng\n" +
                      "- Hủy đặt theo chính sách của từng sân\n" +
                      "- Tuân thủ quy định tại sân\n\n" +
                      "4. Hành vi cấm\n" +
                      "- Spam, lừa đảo\n" +
                      "- Sử dụng thông tin sai sự thật\n" +
                      "- Xâm phạm quyền người khác\n\n" +
                      "5. Từ chối trách nhiệm\n" +
                      "PickleBook là nền tảng kết nối. Chúng tôi không chịu trách nhiệm về chất lượng sân.";

        new AlertDialog.Builder(this)
            .setTitle(R.string.settings_terms)
            .setMessage(terms)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton("Tôi đồng ý", null)
            .show();
    }

    // ─── LOGOUT ──────────────────────────────────────────────────────────────

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.logout_confirm_title)
            .setMessage(R.string.logout_confirm_message)
            .setPositiveButton(R.string.yes, (dialog, which) -> {
                performLogout();
            })
            .setNegativeButton(R.string.no, null)
            .show();
    }

    private void performLogout() {
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut();

        // Clear settings if needed
        // prefs.edit().clear().apply();

        // Navigate to login
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ─── STATIC HELPERS ──────────────────────────────────────────────────────

    /**
     * Get current language setting
     */
    public static String getLanguage(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, "vi");
    }

    /**
     * Check if notifications are enabled
     */
    public static boolean areNotificationsEnabled(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);
        return prefs.getBoolean(KEY_NOTIFICATIONS, true);
    }

    /**
     * Check if dark mode is enabled
     */
    public static boolean isDarkModeEnabled(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    /**
     * Apply saved dark mode on app start
     */
    public static void applySavedDarkMode(android.content.Context context) {
        boolean darkModeEnabled = isDarkModeEnabled(context);
        if (darkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
