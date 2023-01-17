package no.nav.veilarbaktivitet.aktivitetskort.test;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AktivitetskortTestMetrikker {

    private final MeterRegistry meterRegistry;

    public static final String AKTIVITETSKORT_TEST_OPPFOLGINGSPERIODE = "aktivitetskort_test_oppfolgingsperiode";
    public static final String AKTIVITETSKORT_TEST_CASE = "case";
    public static final String AKTIVITETSKORT_TEST_EXCEPTION = "exception";

    public AktivitetskortTestMetrikker(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        meterRegistry.counter(AKTIVITETSKORT_TEST_OPPFOLGINGSPERIODE, AKTIVITETSKORT_TEST_CASE, "");
        meterRegistry.counter(AKTIVITETSKORT_TEST_OPPFOLGINGSPERIODE, AKTIVITETSKORT_TEST_CASE, "1");
        meterRegistry.counter(AKTIVITETSKORT_TEST_OPPFOLGINGSPERIODE, AKTIVITETSKORT_TEST_CASE, "2");
        meterRegistry.counter(AKTIVITETSKORT_TEST_OPPFOLGINGSPERIODE, AKTIVITETSKORT_TEST_CASE, "3");
        meterRegistry.counter(AKTIVITETSKORT_TEST_OPPFOLGINGSPERIODE, AKTIVITETSKORT_TEST_CASE, "4");
        meterRegistry.counter(AKTIVITETSKORT_TEST_OPPFOLGINGSPERIODE, AKTIVITETSKORT_TEST_CASE, "5");
        meterRegistry.counter(AKTIVITETSKORT_TEST_OPPFOLGINGSPERIODE, AKTIVITETSKORT_TEST_CASE, "7");
        meterRegistry.counter(AKTIVITETSKORT_TEST_OPPFOLGINGSPERIODE, AKTIVITETSKORT_TEST_EXCEPTION, "");
    }

    /*
      Case #1: opprettetTidspunkt i en gjeldende periode
      Case #2: opprettetTidspunkt i en gammel periode
      Case #3: opprettetTidspunkt flere matchende perioder
      Case #4: opprettetTidspunkt har ingen perfekt match - det finnes oppfølgingsperiode(r)
      Case #5: bruker har ingen oppfølgingsperioder
      Case #7: opprettettidspunkt 10 minutter innen startdato
     */

    public void countFinnOppfolgingsperiode(int caseNr) {
        Counter.builder(AKTIVITETSKORT_TEST_OPPFOLGINGSPERIODE)
                .tag(AKTIVITETSKORT_TEST_CASE, "" + caseNr)
                .register(meterRegistry)
                .increment();
    }

    public void countError(Exception e) {
        Counter.builder(AKTIVITETSKORT_TEST_OPPFOLGINGSPERIODE)
                .tag(AKTIVITETSKORT_TEST_EXCEPTION, e.getClass().getSimpleName())
                .register(meterRegistry)
                .increment();
    }

}
