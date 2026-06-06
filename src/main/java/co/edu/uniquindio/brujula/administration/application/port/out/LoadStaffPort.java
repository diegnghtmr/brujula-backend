package co.edu.uniquindio.brujula.administration.application.port.out;

import co.edu.uniquindio.brujula.administration.application.dto.StaffPage;
import co.edu.uniquindio.brujula.administration.domain.StaffAccount;
import co.edu.uniquindio.brujula.administration.domain.StaffAccountStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.lang.Nullable;

/** Output port: read staff accounts. */
public interface LoadStaffPort {

  Optional<StaffAccount> findById(UUID id);

  Optional<StaffAccount> findByEmail(String email);

  StaffPage findAll(@Nullable StaffAccountStatus status, int page, int size);
}
