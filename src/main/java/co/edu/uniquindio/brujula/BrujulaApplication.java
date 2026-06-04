package co.edu.uniquindio.brujula;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;

/**
 * Brújula — Curricular Planning Platform.
 *
 * <p>A modular monolith structured with Spring Modulith and hexagonal architecture. Each functional
 * module encapsulates its own domain, application, and infrastructure layers.
 */
@SpringBootApplication
@Modulith
public class BrujulaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BrujulaApplication.class, args);
    }
}
