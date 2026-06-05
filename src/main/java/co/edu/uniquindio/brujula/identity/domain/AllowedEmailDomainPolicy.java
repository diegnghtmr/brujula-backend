package co.edu.uniquindio.brujula.identity.domain;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Domain service: validates an email address against the allowed institutional domain list.
 *
 * <p>RN-007 — only emails whose domain exactly matches one of the allowed entries are permitted.
 * Subdomain emails (e.g. {@code @sub.uniquindio.edu.co}) are NOT allowed unless the subdomain is
 * explicitly in the list. Matching is case-insensitive.
 *
 * <p>Framework-free (ARQ-002). Instantiated by the application service with the current allowed
 * domains loaded from {@link LoadAllowedDomainsPort}.
 */
public final class AllowedEmailDomainPolicy {

  private final Set<String> allowedDomainsLower;

  /**
   * @param allowedDomains non-null, non-empty set of allowed domain hostnames
   * @throws IllegalArgumentException if allowedDomains is null or empty
   */
  public AllowedEmailDomainPolicy(Set<String> allowedDomains) {
    if (allowedDomains == null) {
      throw new IllegalArgumentException("allowedDomains must not be null");
    }
    if (allowedDomains.isEmpty()) {
      throw new IllegalArgumentException(
          "allowedDomains must not be empty — at least one domain is required");
    }
    this.allowedDomainsLower =
        allowedDomains.stream().map(String::toLowerCase).collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Returns {@code true} if the email's domain exactly matches one of the allowed domains.
   *
   * @param email the full email address
   * @throws IllegalArgumentException if email is null
   */
  public boolean isAllowed(String email) {
    if (email == null) {
      throw new IllegalArgumentException("email must not be null");
    }
    int atIndex = email.lastIndexOf('@');
    if (atIndex < 0) {
      return false;
    }
    String domain = email.substring(atIndex + 1).toLowerCase();
    return allowedDomainsLower.contains(domain);
  }
}
