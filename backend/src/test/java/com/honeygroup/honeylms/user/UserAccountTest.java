package com.honeygroup.honeylms.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserAccountTest {

    @Test
    void fullName_joinsFirstAndLastName() {
        UserAccount user = UserAccount.builder().firstName(" Camille ").lastName("Martin").build();
        assertThat(user.fullName()).isEqualTo("Camille Martin");
    }

    @Test
    void fullName_ignoresMissingParts() {
        assertThat(UserAccount.builder().lastName("Martin").build().fullName()).isEqualTo("Martin");
        assertThat(UserAccount.builder().firstName("  ").build().fullName()).isEmpty();
    }
}
