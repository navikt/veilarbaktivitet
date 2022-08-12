package no.nav.veilarbaktivitet.brukernotifikasjon.varsel;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class VarselMetrikk {
    private final MeterRegistry meterRegistry;
    /**
     * brukernotifikasjon_mangler_kvittering teller bestilte varsler der vi ikke har fått kvittering.
     * I prometheus, bruk max() for å finne riktigste verdi, siden de forskjellige nodene kan ha ulike verdier.
     */
    private static final String BRUKERNOTIFIKASJON_MANGLER_KVITTERING = "brukernotifikasjon_mangler_kvittering";
    private AtomicInteger forsinkedeBestillinger = new AtomicInteger();

    public VarselMetrikk(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        Gauge
                .builder(BRUKERNOTIFIKASJON_MANGLER_KVITTERING, forsinkedeBestillinger, AtomicInteger::doubleValue)
                .register(meterRegistry);
    }

    public void countForsinkedeVarslerSisteDognet(int antall) {
        forsinkedeBestillinger.setPlain(antall);
    }

}
