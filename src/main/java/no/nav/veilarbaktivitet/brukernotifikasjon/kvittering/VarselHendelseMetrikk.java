package no.nav.veilarbaktivitet.brukernotifikasjon.kvittering;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.EksternVarselStatus;
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.EksternVarsling;
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.Feilet;
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.VarselHendelseEventType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VarselHendelseMetrikk {
    private final MeterRegistry meterRegistry;
    private static final String EKSTERN_VARSEL_HENDELSE = "ekstern_varsel_hendelse";
    private static final String HENDELSE_TYPE = "hendelse_type";
    private static final String FEILMELDING = "feilmelding";
    private static final List<String> statuser = List.of(
            EksternVarselStatus.bestilt.name(),
            EksternVarselStatus.feilet.name(),
            EksternVarselStatus.sendt.name()
    );

    public VarselHendelseMetrikk(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        statuser.forEach(s -> meterRegistry
                .counter(EKSTERN_VARSEL_HENDELSE, VarselHendelseMetrikk.HENDELSE_TYPE, s, FEILMELDING, ""));
    }

    public void incrementBrukernotifikasjonKvitteringMottatt(EksternVarsling event) {
        var counterBuilder = Counter.builder(EKSTERN_VARSEL_HENDELSE);
        if(event.getHendelseType() == VarselHendelseEventType.feilet_ekstern) {
            counterBuilder.tag(FEILMELDING, ((Feilet) event).getFeilmelding() );
        }
        counterBuilder
                .tag(VarselHendelseMetrikk.HENDELSE_TYPE, event.getHendelseType().name())
                .register(meterRegistry)
                .increment();
    }
}
