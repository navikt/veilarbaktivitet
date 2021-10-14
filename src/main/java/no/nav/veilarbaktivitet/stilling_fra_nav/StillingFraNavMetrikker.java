package no.nav.veilarbaktivitet.stilling_fra_nav;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.veilarbaktivitet.person.InnsenderData;
import org.springframework.stereotype.Component;

@Component
class StillingFraNavMetrikker {
    private final MeterRegistry meterRegistry;
    private static final String stillingFraNavKanDeles = "stilling_fra_nav_kan_deles";
    private static final String stillingFraNavTidsfristUtlopt = "stilling_fra_nav_tidsfrist_utlopt";
    private static final String stillingFraNavManueltAvbrutt = "stilling_fra_nav_manuelt_avbrutt";
    private static final String erEksternBruker = "er_ekstern_bruker";
    private static final String kanDele = "kan_dele";

    StillingFraNavMetrikker(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;


        meterRegistry.counter(stillingFraNavKanDeles, erEksternBruker, "" + true, kanDele, "" + true);
        meterRegistry.counter(stillingFraNavKanDeles, erEksternBruker, "" + true, kanDele, "" + false);
        meterRegistry.counter(stillingFraNavKanDeles, erEksternBruker, "" + false, kanDele, "" + true);
        meterRegistry.counter(stillingFraNavKanDeles, erEksternBruker, "" + false, kanDele, "" + false);
        meterRegistry.counter(stillingFraNavManueltAvbrutt, erEksternBruker, "" + true);
        meterRegistry.counter(stillingFraNavManueltAvbrutt, erEksternBruker, "" + false);

        meterRegistry.counter(stillingFraNavTidsfristUtlopt);
    }


    void countSvar(boolean erEksternBruker, boolean svar) {
        Counter.builder(stillingFraNavKanDeles)
                .tag(StillingFraNavMetrikker.erEksternBruker, "" + erEksternBruker)
                .tag(kanDele, "" + svar)
                .register(meterRegistry)
                .increment();
    }

    void countManuletAvbrutt(InnsenderData brukerType) {
        Counter.builder(stillingFraNavManueltAvbrutt)
                .tag(erEksternBruker, "" + brukerType.equals(InnsenderData.BRUKER))
                .register(meterRegistry)
                .increment();
    }

    void countTidsfristUtlopt() {
        Counter.builder(stillingFraNavTidsfristUtlopt)
                .register(meterRegistry)
                .increment();
    }
}
