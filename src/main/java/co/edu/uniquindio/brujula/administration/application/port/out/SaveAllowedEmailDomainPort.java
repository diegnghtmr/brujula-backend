package co.edu.uniquindio.brujula.administration.application.port.out;

import co.edu.uniquindio.brujula.administration.application.dto.AllowedDomainView;
import java.util.List;
import java.util.UUID;

/** Output port: manage allowed email domain persistence. */
public interface SaveAllowedEmailDomainPort {

  /** Persist a new allowed email domain. */
  AllowedDomainView save(String domain);

  /** Delete an allowed email domain by id. */
  void deleteById(UUID id);

  /** Find all allowed email domains. */
  List<AllowedDomainView> findAll();
}
