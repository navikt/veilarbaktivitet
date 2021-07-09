package no.nav.veilarbaktivitet;


import no.nav.common.utils.SslUtils;
import no.nav.veilarbaktivitet.db.testdriver.TestDriver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@EnableAutoConfiguration
public class VeilarbAktivitetTestApp {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(VeilarbAktivitetTestApp.class);
        application.run(args);
    }
}
