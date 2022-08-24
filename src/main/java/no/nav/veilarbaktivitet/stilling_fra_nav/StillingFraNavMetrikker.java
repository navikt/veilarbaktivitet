package no.nav.veilarbaktivitet.stilling_fra_nav;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.veilarbaktivitet.person.InnsenderData;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class StillingFraNavMetrikker {
    private final MeterRegistry meterRegistry;
    public static final String STILLINGFRANAVOPPRETTET = "stilling_fra_nav_opprettet";
    public static final String STILLINGFRANAVKANDELES = "stilling_fra_nav_kan_deles";
    public static final String STILLINGFRANAVTIDSFRISTUTLOPT = "stilling_fra_nav_tidsfrist_utlopt";
    public static final String STILLINGFRANAVMANUELTAVBRUTT = "stilling_fra_nav_manuelt_avbrutt";
    public static final String REKRUTTERINGSBISTANDSTATUSOPPDATERING = "rekrutteringsbistandstatusoppdatering";
    public static final String IKKEFATTJOBBEN = "ikke_fatt_jobben";
    public static final String EREKSTERNBRUKER = "er_ekstern_bruker";
    public static final String KANDELE = "kan_dele";
    public static final String KANVARSLES = "kan_varsles";
    public static final String SUKSESS = "success";
    public static final String AARSAK = "reason";
    public static final String STATUSOPPDATERINGSTYPE = "statusoppdateringstype";




    StillingFraNavMetrikker(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        meterRegistry.counter(STILLINGFRANAVOPPRETTET, KANVARSLES, "" + true);
        meterRegistry.counter(STILLINGFRANAVOPPRETTET, KANVARSLES, "" + false);
        meterRegistry.counter(STILLINGFRANAVKANDELES, EREKSTERNBRUKER, "" + true, KANDELE, "" + true);
        meterRegistry.counter(STILLINGFRANAVKANDELES, EREKSTERNBRUKER, "" + true, KANDELE, "" + false);
        meterRegistry.counter(STILLINGFRANAVKANDELES, EREKSTERNBRUKER, "" + false, KANDELE, "" + true);
        meterRegistry.counter(STILLINGFRANAVKANDELES, EREKSTERNBRUKER, "" + false, KANDELE, "" + false);
        meterRegistry.counter(STILLINGFRANAVMANUELTAVBRUTT, EREKSTERNBRUKER, "" + true);
        meterRegistry.counter(STILLINGFRANAVMANUELTAVBRUTT, EREKSTERNBRUKER, "" + false);
        meterRegistry.counter(STILLINGFRANAVTIDSFRISTUTLOPT);

        meterRegistry.counter(REKRUTTERINGSBISTANDSTATUSOPPDATERING, SUKSESS, Boolean.toString(true), AARSAK, "", STATUSOPPDATERINGSTYPE, "");
        meterRegistry.counter(REKRUTTERINGSBISTANDSTATUSOPPDATERING, SUKSESS, Boolean.toString(false), AARSAK, "", STATUSOPPDATERINGSTYPE, "");
    }

    void countRekrutteringsbistandStatusoppdatering(boolean success, String reason, RekrutteringsbistandStatusoppdateringEventType type) {
        Counter.builder(REKRUTTERINGSBISTANDSTATUSOPPDATERING)
                .tag(SUKSESS, Boolean.toString(success))
                .tag(AARSAK, Optional.ofNullable(reason).orElse(""))
                .tag(STATUSOPPDATERINGSTYPE, type.name())
                .register(meterRegistry)
                .increment();
    }

    void countStillingFraNavOpprettet(boolean kanVarsles) {
        Counter.builder(STILLINGFRANAVOPPRETTET)
                .tag(StillingFraNavMetrikker.KANVARSLES, "" + kanVarsles)
                .register(meterRegistry)
                .increment();
    }

    void countSvar(boolean erEksternBruker, boolean svar) {
        Counter.builder(STILLINGFRANAVKANDELES)
                .tag(StillingFraNavMetrikker.EREKSTERNBRUKER, "" + erEksternBruker)
                .tag(KANDELE, "" + svar)
                .register(meterRegistry)
                .increment();
    }

    void countManuletAvbrutt(InnsenderData brukerType) {
        Counter.builder(STILLINGFRANAVMANUELTAVBRUTT)
                .tag(EREKSTERNBRUKER, "" + brukerType.equals(InnsenderData.BRUKER))
                .register(meterRegistry)
                .increment();
    }

    void countTidsfristUtlopt() {
        Counter.builder(STILLINGFRANAVTIDSFRISTUTLOPT)
                .register(meterRegistry)
                .increment();
    }
}
