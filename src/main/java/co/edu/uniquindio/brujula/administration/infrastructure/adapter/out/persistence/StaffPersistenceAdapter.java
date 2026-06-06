package co.edu.uniquindio.brujula.administration.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.administration.application.dto.StaffAccountView;
import co.edu.uniquindio.brujula.administration.application.dto.StaffPage;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadAllowedEmailDomainsPort;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadStaffPort;
import co.edu.uniquindio.brujula.administration.application.port.out.SaveStaffPort;
import co.edu.uniquindio.brujula.administration.domain.StaffAccount;
import co.edu.uniquindio.brujula.administration.domain.StaffAccountStatus;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA + JDBC persistence adapter for staff accounts and allowed email domains.
 *
 * <p>Implements {@link SaveStaffPort}, {@link LoadStaffPort}, and {@link
 * LoadAllowedEmailDomainsPort}.
 */
@Component
class StaffPersistenceAdapter implements SaveStaffPort, LoadStaffPort, LoadAllowedEmailDomainsPort {

  private final StaffAccountJpaRepository jpaRepository;
  private final JdbcClient jdbcClient;

  StaffPersistenceAdapter(StaffAccountJpaRepository jpaRepository, JdbcClient jdbcClient) {
    this.jpaRepository = jpaRepository;
    this.jdbcClient = jdbcClient;
  }

  @Override
  @Transactional
  public StaffAccount save(StaffAccount account) {
    // Use save-or-update: if the entity already exists (by id), JPA will update it
    StaffAccountJpaEntity entity = StaffAccountMapper.toEntity(account);
    StaffAccountJpaEntity saved = jpaRepository.save(entity);
    return StaffAccountMapper.toDomain(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<StaffAccount> findById(UUID id) {
    return jpaRepository.findById(id).map(StaffAccountMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<StaffAccount> findByEmail(String email) {
    return jpaRepository.findByEmail(email).map(StaffAccountMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public StaffPage findAll(@Nullable StaffAccountStatus status, int page, int size) {
    PageRequest pageable = PageRequest.of(page, size);
    Page<StaffAccountJpaEntity> jpaPage;
    if (status != null) {
      StaffAccountJpaEntity.StaffStatusJpa jpaStatus =
          status == StaffAccountStatus.ACTIVE
              ? StaffAccountJpaEntity.StaffStatusJpa.ACTIVE
              : StaffAccountJpaEntity.StaffStatusJpa.INACTIVE;
      jpaPage = jpaRepository.findByStatus(jpaStatus, pageable);
    } else {
      jpaPage = jpaRepository.findAll(pageable);
    }
    var items =
        jpaPage.getContent().stream()
            .map(
                e ->
                    new StaffAccountView(
                        e.getId(),
                        e.getEmail(),
                        e.getFullName(),
                        e.getStatus().name(),
                        e.getVersion()))
            .toList();
    return new StaffPage(items, page, size, jpaPage.getTotalElements(), jpaPage.getTotalPages());
  }

  @Override
  @Transactional(readOnly = true)
  public Set<String> loadAllowedDomains() {
    return jdbcClient
        .sql("SELECT domain FROM administration.allowed_email_domain")
        .query(String.class)
        .set()
        .stream()
        .collect(Collectors.toSet());
  }
}
