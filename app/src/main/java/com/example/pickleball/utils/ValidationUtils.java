package com.example.pickleball.utils;

import android.util.Patterns;
import java.util.regex.Pattern;

/**
 * Validation utilities for form inputs
 * Apple-style: Clear, concise, helpful
 */
public class ValidationUtils {

    // ==================== EMAIL VALIDATION ====================

    /**
     * Check if email is valid using Android's Patterns
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    // ==================== PHONE VALIDATION ====================

    /**
     * Check if phone number is valid (10-11 digits, Vietnamese format)
     * Accepts: 0901234567, 0901 234 567, 090-123-4567, +84901234567
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }

        // Remove spaces, dashes, parentheses, plus sign
        String cleanPhone = phone.replaceAll("[\\s\\-()\\+]", "");

        // Remove country code if present (84 for Vietnam)
        if (cleanPhone.startsWith("84")) {
            cleanPhone = "0" + cleanPhone.substring(2);
        }

        // Must be 10-11 digits and start with 0
        return cleanPhone.matches("^0\\d{9,10}$");
    }

    // ==================== PASSWORD VALIDATION ====================

    /**
     * Check if password meets minimum length requirement
     */
    public static boolean isValidPasswordLength(String password) {
        if (password == null) {
            return false;
        }
        return password.length() >= Constants.MIN_PASSWORD_LENGTH;
    }

    /**
     * Check password strength (optional - for better UX)
     * Returns: 0 = weak, 1 = medium, 2 = strong
     */
    public static int getPasswordStrength(String password) {
        if (password == null || password.length() < Constants.MIN_PASSWORD_LENGTH) {
            return 0; // Weak
        }

        int strength = 0;

        // Has uppercase
        if (password.matches(".*[A-Z].*")) strength++;

        // Has lowercase
        if (password.matches(".*[a-z].*")) strength++;

        // Has digit
        if (password.matches(".*\\d.*")) strength++;

        // Has special character
        if (password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) strength++;

        // Length >= 8
        if (password.length() >= 8) strength++;

        // Map to 0-2 scale
        if (strength >= 4) return 2; // Strong
        if (strength >= 2) return 1; // Medium
        return 0; // Weak
    }

    /**
     * Check if two passwords match
     */
    public static boolean doPasswordsMatch(String password, String confirmPassword) {
        if (password == null || confirmPassword == null) {
            return false;
        }
        return password.equals(confirmPassword);
    }

    // ==================== NAME VALIDATION ====================

    /**
     * Check if name is valid (at least 2 characters, no numbers)
     */
    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        String trimmedName = name.trim();

        // At least 2 characters
        if (trimmedName.length() < Constants.MIN_NAME_LENGTH) {
            return false;
        }

        // Should not contain numbers (optional rule)
        // Allow letters, spaces, Vietnamese characters
        return trimmedName.matches("^[a-zA-ZÀ-ỹ\\s]+$");
    }

    // ==================== GENERAL VALIDATION ====================

    /**
     * Check if field is not empty
     */
    public static boolean isNotEmpty(String field) {
        return field != null && !field.trim().isEmpty();
    }

    /**
     * Get error message for email validation
     */
    public static String getEmailError(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "Email không được để trống!";
        }
        if (!isValidEmail(email)) {
            return Constants.ERROR_INVALID_EMAIL;
        }
        return null;
    }

    /**
     * Get error message for phone validation
     */
    public static String getPhoneError(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "Số điện thoại không được để trống!";
        }
        if (!isValidPhone(phone)) {
            return Constants.ERROR_INVALID_PHONE;
        }
        return null;
    }

    /**
     * Get error message for password validation
     */
    public static String getPasswordError(String password) {
        if (password == null || password.isEmpty()) {
            return "Mật khẩu không được để trống!";
        }
        if (!isValidPasswordLength(password)) {
            return Constants.ERROR_PASSWORD_TOO_SHORT;
        }
        return null;
    }

    /**
     * Get error message for name validation
     */
    public static String getNameError(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Tên không được để trống!";
        }
        if (!isValidName(name)) {
            return "Tên phải có ít nhất 2 ký tự và không chứa số!";
        }
        return null;
    }

    // Private constructor
    private ValidationUtils() {
        throw new AssertionError("Cannot instantiate ValidationUtils class");
    }
}
