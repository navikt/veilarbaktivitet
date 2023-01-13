package no.nav.veilarbaktivitet.aktivitetskort.test;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AktivitetskortTestMetrikker {

    private final MeterRegistry meterRegistry;

    public static final String AKTIVITETSKORT_TEST_OPPFOLGINGSPERIODE = "aktivitetskort_test_oppfolgingsperiode";

    public AktivitetskortTestMetrikker(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /*
      Case #1: opprettetTidspunkt i en gjeldende periode
      Case #2: opprettetTidspunkt i en gammel periode
      Case #3: opprettetTidspunkt flere matchende perioder
      Case #4: opprettetTidspunkt har ingen perfekt match (det finnes oppfølgingsperiode(r))
      Case #5: bruker har ingen oppfølgingsperioder
      Case #6: illegalstateexception, skal aldri skje forhåpentligvis
     */

    public void countFinnOppfolgingsperiode(int caseNr) {
        Counter.builder(AKTIVITETSKORT_TEST_OPPFOLGINGSPERIODE)
                .tag("case", "" + caseNr)
                .register(meterRegistry)
                .increment();
    }

}
