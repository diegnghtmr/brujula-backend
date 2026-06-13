package co.edu.uniquindio.brujula.audit.infrastructure.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for audit events.
 *
 * <p>Package-private — only the adapter uses this; no cross-module access.
 */
interface AuditEventJpaRepository extends JpaRepository<AuditEventJpaEntity, UUID> {}
