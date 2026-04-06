package com.swp391.condocare_swp.util;

import java.security.SecureRandom;

/**
 * PasswordUtils — Tiện ích tạo mật khẩu ngẫu nhiên dùng chung.
 *
 * Lý do tách class này:
 *   generateRandomPassword() bị copy-paste giống hệt nhau ở:
 *     - StaffManagementService.generateRandomPassword()
 *     - ResidentManagementService.generateRandomPassword()
 *   → Tách ra đây để cả 2 service cùng dùng, tránh diverge về sau.
 *
 * Cách dùng:
 *   String pass = PasswordUtils.generateRandomPassword();
 */
public final class PasswordUtils {

    // Bộ ký tự tạo mật khẩu: loại bỏ các ký tự dễ nhầm (0/O, 1/l/I)
    private static final String ALLOWED_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#$%";

    private static final int PASSWORD_LENGTH = 12;

    private PasswordUtils() {
        // Utility class — không khởi tạo
    }

    /**
     * Sinh mật khẩu ngẫu nhiên dài 12 ký tự,
     * sử dụng SecureRandom để đảm bảo tính bảo mật.
     *
     * Bộ ký tự: chữ hoa, chữ thường (bỏ ký tự dễ nhầm), số, và @#$%
     *
     * @return Chuỗi mật khẩu 12 ký tự
     */
    public static String generateRandomPassword() {
        SecureRandom rng = new SecureRandom();
        char[] chars = new char[PASSWORD_LENGTH];
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            chars[i] = ALLOWED_CHARS.charAt(rng.nextInt(ALLOWED_CHARS.length()));
        }
        return new String(chars);
    }
}