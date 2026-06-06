package co.edu.uniquindio.brujula.administration.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.administration.domain.StaffAccount;
import co.edu.uniquindio.brujula.administration.domain.StaffAccountStatus;

/** Stateless mapper between StaffAccount domain and JPA entity. Package-private. */
final class StaffAccountMapper {

  private StaffAccountMapper() {}

  static StaffAccountJpaEntity toEntity(StaffAccount account) {
    StaffAccountJpaEntity entity =
        new StaffAccountJpaEntity(
            account.getId(),
            account.getEmail(),
            account.getFullName(),
            toJpaStatus(account.getStatus()));
    // If status differs from default ACTIVE, update it
    if (account.getStatus() == StaffAccountStatus.INACTIVE) {
      entity.setStatus(StaffAccountJpaEntity.StaffStatusJpa.INACTIVE);
    }
    return entity;
  }

  static StaffAccount toDomain(StaffAccountJpaEntity entity) {
    return StaffAccount.reconstitute(
        entity.getId(),
        entity.getEmail(),
        entity.getFullName(),
        toDomainStatus(entity.getStatus()),
        entity.getVersion());
  }

  private static StaffAccountJpaEntity.StaffStatusJpa toJpaStatus(StaffAccountStatus status) {
    return switch (status) {
      case ACTIVE -> StaffAccountJpaEntity.StaffStatusJpa.ACTIVE;
      case INACTIVE -> StaffAccountJpaEntity.StaffStatusJpa.INACTIVE;
    };
  }

  private static StaffAccountStatus toDomainStatus(StaffAccountJpaEntity.StaffStatusJpa status) {
    return switch (status) {
      case ACTIVE -> StaffAccountStatus.ACTIVE;
      case INACTIVE -> StaffAccountStatus.INACTIVE;
    };
  }
}
