package vn.edu.fpt.myschool.auth.domain;

import java.util.Optional;
import java.util.regex.Pattern;

public record VietnamesePhoneNumber(String value) {

    private static final Pattern SEPARATORS = Pattern.compile("[\\s.()\\-]");
    private static final Pattern MOBILE_NUMBER = Pattern.compile("^0[35789][0-9]{8}$");

    public VietnamesePhoneNumber {
        if (!MOBILE_NUMBER.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid Vietnamese mobile phone number");
        }
    }

    public static VietnamesePhoneNumber normalize(String rawValue) {
        return tryNormalize(rawValue)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid Vietnamese mobile phone number"));
    }

    public static Optional<VietnamesePhoneNumber> tryNormalize(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }

        String compact = SEPARATORS.matcher(rawValue.strip()).replaceAll("");
        if (compact.startsWith("+84")) {
            compact = "0" + compact.substring(3);
        } else if (compact.startsWith("0084")) {
            compact = "0" + compact.substring(4);
        } else if (compact.startsWith("84") && compact.length() == 11) {
            compact = "0" + compact.substring(2);
        }

        if (!MOBILE_NUMBER.matcher(compact).matches()) {
            return Optional.empty();
        }
        return Optional.of(new VietnamesePhoneNumber(compact));
    }
}
