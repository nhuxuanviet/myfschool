package vn.edu.fpt.myschool.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class VietnamesePhoneNumberTest {

    @ParameterizedTest
    @CsvSource({
        "0912345678,0912345678",
        "+84912345678,0912345678",
        "84912345678,0912345678",
        "0084912345678,0912345678",
        "'0912 345 678',0912345678",
        "'0912.345.678',0912345678"
    })
    void normalizesSupportedVietnameseMobileFormats(String input, String expected) {
        assertThat(VietnamesePhoneNumber.normalize(input).value()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "0212345678", "+841234", "09123456789"})
    void rejectsInvalidPhoneNumbers(String input) {
        assertThat(VietnamesePhoneNumber.tryNormalize(input)).isEmpty();
    }
}
