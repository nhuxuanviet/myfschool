package vn.edu.fpt.myschool.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"Student@123", "MậtKhẩu@123", "Another#Password7"})
    void acceptsStrongPasswords(String password) {
        assertThat(PasswordPolicy.isStrong(password)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "short1!",
        "alllowercase1!",
        "ALLUPPERCASE1!",
        "NoDigits!",
        "NoSpecial123",
        "Has Space@123"
    })
    void rejectsWeakPasswords(String password) {
        assertThat(PasswordPolicy.isStrong(password)).isFalse();
    }
}
