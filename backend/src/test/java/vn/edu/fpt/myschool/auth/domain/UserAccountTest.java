package vn.edu.fpt.myschool.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class UserAccountTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private static UserAccount accountWith(Set<UserRole> roles) {
        return new UserAccount(USER_ID, "0912345678", "hash", roles, true);
    }

    @Test
    void rejectsAnAccountWithoutAnyRole() {
        assertThatThrownBy(() -> accountWith(Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one role");
    }

    @Test
    void rejectsNullRoles() {
        assertThatThrownBy(() -> accountWith(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void exposesRolesAsAnImmutableCopy() {
        Set<UserRole> mutable = new LinkedHashSet<>(Set.of(UserRole.TEACHER));
        UserAccount account = accountWith(mutable);

        mutable.add(UserRole.ADMIN);

        assertThat(account.roles()).containsExactly(UserRole.TEACHER);
        assertThatThrownBy(() -> account.roles().add(UserRole.ADMIN))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void confirmsWhetherTheAccountHoldsARole() {
        UserAccount account = accountWith(Set.of(UserRole.TEACHER, UserRole.PARENT));

        assertThat(account.hasRole(UserRole.TEACHER)).isTrue();
        assertThat(account.hasRole(UserRole.PARENT)).isTrue();
        assertThat(account.hasRole(UserRole.ADMIN)).isFalse();
        assertThat(account.hasRole(UserRole.STUDENT)).isFalse();
    }

    @Test
    void carriesEveryRoleAnAccountHolds() {
        UserAccount account = accountWith(Set.of(UserRole.TEACHER, UserRole.PARENT));

        assertThat(account.roles())
                .containsExactlyInAnyOrder(UserRole.TEACHER, UserRole.PARENT);
    }
}
