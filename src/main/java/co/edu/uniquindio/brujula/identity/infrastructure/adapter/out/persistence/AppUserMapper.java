package co.edu.uniquindio.brujula.identity.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.identity.domain.AccountStatus;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import co.edu.uniquindio.brujula.identity.domain.OnboardingState;
import co.edu.uniquindio.brujula.identity.domain.Role;
import co.edu.uniquindio.brujula.identity.infrastructure.adapter.out.persistence.AppUserJpaEntity.AccountStatusJpa;
import co.edu.uniquindio.brujula.identity.infrastructure.adapter.out.persistence.AppUserJpaEntity.RoleJpa;

/**
 * Mapper between {@link AppUser} domain objects and {@link AppUserJpaEntity} JPA entities.
 *
 * <p>Confined to the persistence adapter package (ARQ-005 / ARQ-006). Updated for V008 fields:
 * consent evidence (consentTimestamp, termsVersion, privacyVersion) and onboardingState.
 */
final class AppUserMapper {

  private AppUserMapper() {}

  static AppUser toDomain(AppUserJpaEntity entity) {
    OnboardingState onboardingState = toOnboardingState(entity.getOnboardingState());

    return AppUser.reconstitute(
        entity.getId(),
        entity.getOidcSubject(),
        entity.getEmail(),
        entity.getFullName(),
        toRole(entity.getRole()),
        toStatus(entity.getStatus()),
        entity.isOnboardingCompleted(),
        entity.getVersion(),
        onboardingState,
        entity.getConsentTimestamp(),
        entity.getTermsVersion(),
        entity.getPrivacyVersion());
  }

  static AppUserJpaEntity toEntity(AppUser user) {
    var entity =
        new AppUserJpaEntity(
            user.id(),
            user.oidcSubject(),
            user.email(),
            user.fullName(),
            toRoleJpa(user.role()),
            toStatusJpa(user.status()),
            user.onboardingCompleted(),
            user.onboardingState() != null
                ? user.onboardingState().name()
                : OnboardingState.ACTIVE.name());

    entity.setConsentTimestamp(user.consentTimestamp());
    entity.setTermsVersion(user.termsVersion());
    entity.setPrivacyVersion(user.privacyVersion());

    return entity;
  }

  private static Role toRole(RoleJpa jpaRole) {
    return switch (jpaRole) {
      case STUDENT -> Role.STUDENT;
      case STAFF -> Role.STAFF;
      case ADMIN -> Role.ADMIN;
    };
  }

  private static RoleJpa toRoleJpa(Role role) {
    return switch (role) {
      case STUDENT -> RoleJpa.STUDENT;
      case STAFF -> RoleJpa.STAFF;
      case ADMIN -> RoleJpa.ADMIN;
    };
  }

  private static AccountStatus toStatus(AccountStatusJpa jpa) {
    return switch (jpa) {
      case ACTIVE -> AccountStatus.ACTIVE;
      case INACTIVE -> AccountStatus.INACTIVE;
      case PENDING_VERIFICATION -> AccountStatus.PENDING_VERIFICATION;
    };
  }

  private static AccountStatusJpa toStatusJpa(AccountStatus status) {
    return switch (status) {
      case ACTIVE -> AccountStatusJpa.ACTIVE;
      case INACTIVE -> AccountStatusJpa.INACTIVE;
      case PENDING_VERIFICATION -> AccountStatusJpa.PENDING_VERIFICATION;
    };
  }

  private static OnboardingState toOnboardingState(String value) {
    if (value == null || value.isBlank()) {
      return OnboardingState.ACTIVE;
    }
    try {
      return OnboardingState.valueOf(value);
    } catch (IllegalArgumentException e) {
      // Unknown value from DB — default to ACTIVE for safety.
      return OnboardingState.ACTIVE;
    }
  }
}
