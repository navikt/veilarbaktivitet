package no.nav.veilarbaktivitet.stilling_fra_nav;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.veilarbaktivitet.person.InnsenderData;
import org.springframework.stereotype.Component;

@Component
class StillingFraNavMetrikker {
    private final MeterRegistry meterRegistry;

    StillingFraNavMetrikker(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        meterRegistry.counter("stilling_fra_nav_kan_deles", "er_ekstern_bruker", "" + true, "kan_dele", "" + true);
        meterRegistry.counter("stilling_fra_nav_kan_deles", "er_ekstern_bruker", "" + true, "kan_dele", "" + false);
        meterRegistry.counter("stilling_fra_nav_kan_deles", "er_ekstern_bruker", "" + false, "kan_dele", "" + true);
        meterRegistry.counter("stilling_fra_nav_kan_deles", "er_ekstern_bruker", "" + false, "kan_dele", "" + false);
        meterRegistry.counter("stilling_fra_nav_manuelt_avbrutt", "er_ekstern_bruker", "" + true);
        meterRegistry.counter("stilling_fra_nav_manuelt_avbrutt", "er_ekstern_bruker", "" + false);
        meterRegistry.counter("stilling_fra_nav_tidsfrist_utlopt");
    }


    void countSvar(boolean erEksternBruker, boolean svar) {
        Counter.builder("StillingFraNavKanDeles")
                .tag("erEksternBruker", "" + erEksternBruker)
                .tag("kanDele", "" + svar)
                .register(meterRegistry)
                .increment();
    }

    void countManuletAvbrutt(InnsenderData brukerType) {
        Counter.builder("StillingFraNavManueltAvbrutt")
                .tag("erEksternBruker", "" + brukerType.equals(InnsenderData.BRUKER))
                .register(meterRegistry)
                .increment();
    }

    void countTidsfristUtlopt() {
        Counter.builder("StillingFraNavTidsfristUtlopt")
                .register(meterRegistry)
                .increment();
    }
}
