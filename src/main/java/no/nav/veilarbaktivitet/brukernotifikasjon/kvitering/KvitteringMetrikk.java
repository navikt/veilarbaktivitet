package no.nav.veilarbaktivitet.brukernotifikasjon.kvitering;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class KvitteringMetrikk {
    private final MeterRegistry meterRegistry;
    private static final String BRUKERNOTIFIKASJON_KVITTERING_MOTTATT = "brukernotifikasjon_kvittering_mottatt";
    private static final String BRUKERNOTIFIKASJON_KVITTERING_TID_BRUKT = "brukernotifikasjon_kvittering_tid_brukt";
    private static final String STATUS = "status";
    private static final List<String> statuser = List.of(
            EksternVarslingKvitteringConsumer.FEILET,
            EksternVarslingKvitteringConsumer.FERDIGSTILT,
            EksternVarslingKvitteringConsumer.INFO,
            EksternVarslingKvitteringConsumer.OVERSENDT
    );
    private static final String INTERVAL_NAVN = "interval_navn";
    public enum IntervalNavn {
        FORSOKT_SENDT_BEKREFTET_SENDT_DIFF("forsokt_sendt_bekreftet_sendt_diff");

        public final String navn;

        IntervalNavn(String navn) {
            this.navn = navn;
        }
    }

    public KvitteringMetrikk(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        statuser.forEach(s -> meterRegistry.counter(BRUKERNOTIFIKASJON_KVITTERING_MOTTATT, KvitteringMetrikk.STATUS, s));
        for (IntervalNavn navn : IntervalNavn.values()) {
            meterRegistry.timer(BRUKERNOTIFIKASJON_KVITTERING_TID_BRUKT, INTERVAL_NAVN, navn.navn);
        }
    }

    public void incrementBrukernotifikasjonKvitteringMottatt(String status) {
        Counter.builder(BRUKERNOTIFIKASJON_KVITTERING_MOTTATT)
                .tag(KvitteringMetrikk.STATUS, status)
                .register(meterRegistry)
                .increment();
    }

    public void registrerTidBrukt(IntervalNavn navn, Duration tidBrukt) {
        Timer.builder(BRUKERNOTIFIKASJON_KVITTERING_TID_BRUKT)
                .tag(INTERVAL_NAVN, navn.navn)
                .register(meterRegistry)
                .record(tidBrukt);
    }
}
