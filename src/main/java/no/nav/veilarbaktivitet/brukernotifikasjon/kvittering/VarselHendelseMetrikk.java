package no.nav.veilarbaktivitet.brukernotifikasjon.kvittering;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.EksternVarselStatus;
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.VarselHendelseEventType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VarselHendelseMetrikk {
    private final MeterRegistry meterRegistry;
    private static final String BRUKERNOTIFIKASJON_KVITTERING_MOTTATT = "varsel_hendelse_mottatt";
    private static final String HENDELSE_TYPE = "hendelse_type";
    private static final List<String> statuser = List.of(
            EksternVarselStatus.bestilt.name(),
            EksternVarselStatus.feilet.name(),
            EksternVarselStatus.sendt.name()
    );

    public VarselHendelseMetrikk(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        statuser.forEach(s -> meterRegistry.counter(BRUKERNOTIFIKASJON_KVITTERING_MOTTATT, VarselHendelseMetrikk.HENDELSE_TYPE, s));
    }

    public void incrementBrukernotifikasjonKvitteringMottatt(VarselHendelseEventType eventType) {
        Counter.builder(BRUKERNOTIFIKASJON_KVITTERING_MOTTATT)
                .tag(VarselHendelseMetrikk.HENDELSE_TYPE, eventType.name())
                .register(meterRegistry)
                .increment();
    }
}
