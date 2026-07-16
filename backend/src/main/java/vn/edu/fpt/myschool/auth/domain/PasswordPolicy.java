package vn.edu.fpt.myschool.auth.domain;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {

    private static final int MINIMUM_LENGTH = 8;
    private static final int MAXIMUM_BCRYPT_BYTES = 72;

    private PasswordPolicy() {
    }

    public static boolean isStrong(String password) {
        if (password == null
                || password.length() < MINIMUM_LENGTH
                || !isBcryptCompatible(password)) {
            return false;
        }

        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (int index = 0; index < password.length(); index++) {
            char character = password.charAt(index);
            if (Character.isWhitespace(character)) {
                return false;
            }
            hasUppercase |= Character.isUpperCase(character);
            hasLowercase |= Character.isLowerCase(character);
            hasDigit |= Character.isDigit(character);
            hasSpecial |= !Character.isLetterOrDigit(character);
        }
        return hasUppercase && hasLowercase && hasDigit && hasSpecial;
    }

    public static boolean isBcryptCompatible(String password) {
        return password != null
                && password.getBytes(StandardCharsets.UTF_8).length <= MAXIMUM_BCRYPT_BYTES;
    }
}
