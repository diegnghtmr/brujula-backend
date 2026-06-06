package co.edu.uniquindio.brujula.administration.application.port.out;

import co.edu.uniquindio.brujula.administration.domain.StaffAccount;

/** Output port: persist staff accounts. */
public interface SaveStaffPort {

  StaffAccount save(StaffAccount account);
}
