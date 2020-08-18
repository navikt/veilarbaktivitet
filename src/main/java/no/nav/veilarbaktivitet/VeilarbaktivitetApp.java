package no.nav.veilarbaktivitet;

import no.nav.common.utils.SslUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VeilarbaktivitetApp {
    public static void main(String... args) {
        SslUtils.setupTruststore();
        SpringApplication.run(VeilarbaktivitetApp.class, args);
    }
}
