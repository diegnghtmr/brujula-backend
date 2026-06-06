/**
 * Application-layer DTOs for the audit module.
 *
 * <p>Named interface {@code spi}: exposed alongside {@code application.port.in} so that other
 * modules can construct {@link
 * co.edu.uniquindio.brujula.audit.application.dto.RecordAuditEventCommand} and consume {@link
 * co.edu.uniquindio.brujula.audit.application.dto.AuditEventItem} results.
 */
@org.springframework.modulith.NamedInterface(name = "spi-dto")
package co.edu.uniquindio.brujula.audit.application.dto;
