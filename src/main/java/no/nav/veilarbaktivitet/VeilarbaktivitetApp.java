package no.nav.veilarbaktivitet;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import no.nav.common.utils.SslUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtLeastFor = "${shedlock.lockAtLeastFor}", defaultLockAtMostFor = "PT10M")
public class VeilarbaktivitetApp {
    public static void main(String... args) {
        SslUtils.setupTruststore();
        SpringApplication.run(VeilarbaktivitetApp.class, args);
    }
}
