# Activity Package Structure

This document describes the organization of Activity classes in the PickleBall app.

## 📁 Package Organization

```
activity/
├── auth/          ← MODULE 1: Authentication & Onboarding
│   ├── SplashActivity.java
│   ├── OnboardingActivity.java
│   ├── LoginActivity.java
│   ├── RegisterActivity.java
│   └── ForgotPasswordActivity.java
│
├── profile/       ← MODULE 1: User Profile Management
│   └── ProfileActivity.java
│
├── home/          ← Dashboard screens (Role-based)
│   ├── CustomerHomeActivity.java
│   ├── OwnerHomeActivity.java
│   └── AdminHomeActivity.java
│
└── court/         ← MODULE 2: Court Management
    ├── CourtDetailActivity.java
    └── AddCourtActivity.java
```

---

## 📦 Package Details

### 🔐 `auth/` - Authentication Module
**Purpose**: Handles all authentication and initial app flow

| Activity | Description | Key Features |
|----------|-------------|--------------|
| `SplashActivity` | Entry point, shows logo | - Auto-login check<br>- Onboarding redirect<br>- Role-based navigation |
| `OnboardingActivity` | First-time user guide (3 slides) | - ViewPager2<br>- SharedPreferences flag<br>- Skip button |
| `LoginActivity` | Email/Password & Google Sign-In | - Form validation<br>- Google OAuth<br>- Error handling |
| `RegisterActivity` | New user registration | - Email validation<br>- Phone validation<br>- Password strength<br>- Confirm password |
| `ForgotPasswordActivity` | Password reset via email | - Email validation<br>- Firebase reset link |

**Related Utilities**:
- `utils/ValidationUtils.java` - Form validation helpers
- `utils/Constants.java` - Role constants, error messages

---

### 👤 `profile/` - Profile Management
**Purpose**: User profile viewing and editing

| Activity | Description | Key Features |
|----------|-------------|--------------|
| `ProfileActivity` | View/Edit user profile | - Update name & phone<br>- Change password<br>- Upload avatar (TODO)<br>- Logout |

**Upcoming Features** (Module 1.11-1.15):
- [ ] Avatar upload (Firebase Storage)
- [ ] Settings screen
- [ ] Language selection
- [ ] Dark mode toggle

---

### 🏠 `home/` - Dashboard (Role-based)
**Purpose**: Main landing screens after login, different for each role

| Activity | Role | Description |
|----------|------|-------------|
| `CustomerHomeActivity` | `user` | Customer dashboard with quick actions |
| `OwnerHomeActivity` | `owner` | Court owner dashboard with stats |
| `AdminHomeActivity` | `admin` | Admin panel with overview |

**Navigation Logic**:
```java
// In SplashActivity.java
public static void navigateByRole(Context ctx, String role) {
    Intent intent;
    if ("admin".equals(role)) {
        intent = new Intent(ctx, AdminHomeActivity.class);
    } else if ("owner".equals(role)) {
        intent = new Intent(ctx, OwnerHomeActivity.class);
    } else {
        intent = new Intent(ctx, CustomerHomeActivity.class);
    }
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    ctx.startActivity(intent);
}
```

---

### 🏟️ `court/` - Court Management (Module 2)
**Purpose**: Court listing, details, and management

| Activity | Description | Access |
|----------|-------------|--------|
| `CourtDetailActivity` | View court details, book slots | All users |
| `AddCourtActivity` | Add/edit court information | Owner only |

**Upcoming Features** (Module 2):
- [ ] Google Maps integration
- [ ] Court search & filter
- [ ] Court reviews & ratings
- [ ] Favorites

---

## 🔄 Migration Notes

### Before (Old Structure)
```
activity/
├── LoginActivity.java
├── RegisterActivity.java
├── ProfileActivity.java
├── CustomerHomeActivity.java
└── ... (all mixed together)
```

### After (New Structure)
```
activity/
├── auth/
│   └── LoginActivity.java
├── profile/
│   └── ProfileActivity.java
└── home/
    └── CustomerHomeActivity.java
```

### Package Declaration Changes
All files have updated package declarations:
```java
// Old
package com.example.pickleball.activity;

// New
package com.example.pickleball.activity.auth;
package com.example.pickleball.activity.profile;
package com.example.pickleball.activity.home;
package com.example.pickleball.activity.court;
```

### AndroidManifest.xml Updates
```xml
<!-- Old -->
<activity android:name=".activity.LoginActivity" />

<!-- New -->
<activity android:name=".activity.auth.LoginActivity" />
```

---

## 📋 Module 1 Progress Checklist

### ✅ Completed (1.1 - 1.10)
- [x] 1.1 Register (Email + Password)
- [x] 1.2 Login (Email + Password)
- [x] 1.3 Forgot Password
- [x] 1.4 View Profile
- [x] 1.5 Change Password
- [x] 1.6 Logout
- [x] 1.7 Role-based navigation (3 roles)
- [x] 1.8 Splash Screen
- [x] 1.9 Onboarding (3 slides)
- [x] 1.10 Google Sign-In
- [x] **BONUS**: Form validation (ValidationUtils)
- [x] **BONUS**: Constants centralization

### 🚧 In Progress (1.11 - 1.15)
- [ ] 1.11 Upload Avatar (Firebase Storage)
- [ ] 1.12 Dashboard hoàn chỉnh theo role
- [ ] 1.13 Full validation (✅ Done - RegisterActivity & LoginActivity)
- [ ] 1.14 Remember Me / Auto-login (✅ Done - SplashActivity)
- [ ] 1.15 Settings Activity

### 📍 Next Steps
1. **Avatar Upload** - Firebase Storage integration
2. **Settings Screen** - Notifications, Language, Dark mode
3. **Dashboard Enhancement** - Real data, stats, charts
4. **Google Maps** - Court locations

---

## 🎨 Design System

Following **Apple Design Language**:
- **Colors**: Green primary (`#34C759`)
- **Typography**: SF Pro Text style
- **Spacing**: 16dp, 24dp increments
- **Border Radius**: 12dp inputs, 20dp cards, 100dp buttons (pill)
- **Elevation**: Flat design (0dp), subtle borders instead

See: `/DESIGN-apple.md` for full design specs

---

## 🔗 Related Files

### Utils
- `utils/Constants.java` - App constants
- `utils/ValidationUtils.java` - Form validation
- `utils/FirebaseHelper.java` - Firebase operations

### Models
- `model/User.java` - User data model
- `model/Court.java` - Court data model
- `model/Booking.java` - Booking data model

### Resources
- `res/values/colors.xml` - Green color palette
- `res/values/strings.xml` - Localized strings
- `res/values/themes.xml` - Material theme + custom styles

---

## 📝 Coding Standards

### Naming Conventions
- Activities: `{Feature}Activity.java`
- Layouts: `activity_{feature}.xml`
- IDs: `{type}{Feature}` (e.g., `btnLogin`, `edtEmail`)

### Import Organization
```java
// 1. Android imports
import android.content.Intent;
import android.os.Bundle;

// 2. AndroidX imports
import androidx.appcompat.app.AppCompatActivity;

// 3. App imports
import com.example.pickleball.R;
import com.example.pickleball.activity.auth.LoginActivity;
import com.example.pickleball.utils.Constants;

// 4. Third-party imports
import com.google.firebase.auth.FirebaseAuth;
```

### Code Style
- Use `Constants` for string literals
- Use `ValidationUtils` for form validation
- Comment in Vietnamese for teammates (optional)
- Follow Apple-style UX: clear, minimal, helpful errors

---

**Last Updated**: Module 1 Restructure - 2024
**Maintainer**: PickleBall Dev Team