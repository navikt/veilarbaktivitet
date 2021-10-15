package no.nav.veilarbaktivitet.brukernotifikasjon.kvitering;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KvitteringMetrikk {
    private final MeterRegistry meterRegistry;
    private static final String brukernotifikasjonKvitteringMottatt = "brukernotifikasjon_kvittering_mottatt";
    private static final String status = "status";
    private static final List<String> statuser = List.of(
            EksternVarslingKvitteringConsumer.FEILET,
            EksternVarslingKvitteringConsumer.FERDISTSTILT,
            EksternVarslingKvitteringConsumer.INFO,
            EksternVarslingKvitteringConsumer.OVERSENDT
    );

    public KvitteringMetrikk(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        statuser.forEach(s -> meterRegistry.counter(brukernotifikasjonKvitteringMottatt, KvitteringMetrikk.status, s));
    }

    public void incrementBrukernotifikasjonKvitteringMottatt(String status) {
        Counter.builder(brukernotifikasjonKvitteringMottatt)
                .tag(KvitteringMetrikk.status, status)
                .register(meterRegistry)
                .increment();
    }
}
