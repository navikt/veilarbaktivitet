package no.nav.veilarbaktivitet.stilling_fra_nav;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StillingFraNavMetriker {
    private final MeterRegistry meterRegistry;

    void countSvar(boolean erEksternBruker, boolean svar) {
        Counter.builder("StillingFraNavKanDeles").tag("erEksternBruker", "" + erEksternBruker).tag("kanDele", "" + svar).register(meterRegistry).increment();
    }
}
