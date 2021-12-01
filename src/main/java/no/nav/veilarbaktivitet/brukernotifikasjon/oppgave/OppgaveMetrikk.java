package no.nav.veilarbaktivitet.brukernotifikasjon.oppgave;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OppgaveMetrikk {
    private final MeterRegistry meterRegistry;
    private static final String BRUKERNOTIFIKASJON_FORSOKT_SENDT = "brukernotifikasjon_forsokt_sendt";

    public OppgaveMetrikk(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        meterRegistry.counter(BRUKERNOTIFIKASJON_FORSOKT_SENDT);
    }

    public void countForsinkedeVarslerSisteDognet(long antall) {
        Counter.builder(BRUKERNOTIFIKASJON_FORSOKT_SENDT)
                .register(meterRegistry)
                .increment(antall);
    }
}
