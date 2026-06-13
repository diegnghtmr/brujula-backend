package co.edu.uniquindio.brujula.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * RED → GREEN domain unit tests for AllowedEmailDomainPolicy.
 *
 * <p>Pure JUnit 5 — zero Spring context. Validates institutional domain enforcement.
 */
class AllowedEmailDomainPolicyTest {

  @Test
  void uniquindio_domain_is_allowed() {
    var policy = new AllowedEmailDomainPolicy(Set.of("uniquindio.edu.co"));
    assertThat(policy.isAllowed("student@uniquindio.edu.co")).isTrue();
  }

  @Test
  void gmail_domain_is_rejected() {
    var policy = new AllowedEmailDomainPolicy(Set.of("uniquindio.edu.co"));
    assertThat(policy.isAllowed("user@gmail.com")).isFalse();
  }

  @Test
  void email_with_allowed_subdomain_is_rejected_when_only_root_domain_allowed() {
    var policy = new AllowedEmailDomainPolicy(Set.of("uniquindio.edu.co"));
    assertThat(policy.isAllowed("user@sub.uniquindio.edu.co")).isFalse();
  }

  @Test
  void multiple_allowed_domains_are_all_accepted() {
    var policy =
        new AllowedEmailDomainPolicy(Set.of("uniquindio.edu.co", "otheruniversity.edu.co"));
    assertThat(policy.isAllowed("a@uniquindio.edu.co")).isTrue();
    assertThat(policy.isAllowed("b@otheruniversity.edu.co")).isTrue();
  }

  @Test
  void email_not_in_any_allowed_domain_is_rejected() {
    var policy =
        new AllowedEmailDomainPolicy(Set.of("uniquindio.edu.co", "otheruniversity.edu.co"));
    assertThat(policy.isAllowed("x@third.com")).isFalse();
  }

  @Test
  void null_email_throws_illegal_argument() {
    var policy = new AllowedEmailDomainPolicy(Set.of("uniquindio.edu.co"));
    assertThatThrownBy(() -> policy.isAllowed(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void empty_allowed_domains_throws_on_construction() {
    // Domain invariant: empty allowed-domain set is invalid
    assertThatThrownBy(() -> new AllowedEmailDomainPolicy(Set.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("allowedDomains");
  }

  @Test
  void null_allowed_domains_throws_on_construction() {
    assertThatThrownBy(() -> new AllowedEmailDomainPolicy(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void domain_matching_is_case_insensitive() {
    var policy = new AllowedEmailDomainPolicy(Set.of("uniquindio.edu.co"));
    assertThat(policy.isAllowed("User@UNIQUINDIO.EDU.CO")).isTrue();
  }
}
