package no.nav.veilarbaktivitet;

import no.nav.veilarbaktivitet.config.ApplicationTestConfig;
import no.nav.veilarbaktivitet.db.testdriver.TestDriver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;


@EnableAutoConfiguration
@Import(ApplicationTestConfig.class)
public class VeilarbAktivitetTestApp {

    public static void main(String[] args) throws Exception {
        // We need to initialize the driver before spring starts or Flyway will not be able to use the driver
        TestDriver.init();

        SpringApplication application = new SpringApplication(VeilarbAktivitetTestApp.class);
        application.setAdditionalProfiles("local");
        application.run(args);
    }
}
