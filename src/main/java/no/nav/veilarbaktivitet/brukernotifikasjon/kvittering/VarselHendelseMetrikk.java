package no.nav.veilarbaktivitet.brukernotifikasjon.kvittering;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import kotlin.enums.EnumEntries;
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.EksternVarsling;
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.Feilet;
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.InternVarselHendelseDTO;
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.VarselHendelseEventType;
import org.springframework.stereotype.Component;

@Component
public class VarselHendelseMetrikk {
    private final MeterRegistry meterRegistry;
    public static final String EKSTERN_VARSEL_HENDELSE = "ekstern_varsel_hendelse";
    private static final String HENDELSE_TYPE = "hendelse_type";
    private static final String FEILMELDING = "feilmelding";
    private static final EnumEntries<VarselHendelseEventType> statuser = VarselHendelseEventType.getEntries();
            /*
            Eksterne hendelse (tidligere kalt kvittering)
                VarselHendelseEventType.bestilt_ekstern.name(),
                VarselHendelseEventType.feilet_ekstern.name(),
                VarselHendelseEventType.sendt_ekstern.name(),
                VarselHendelseEventType.renotifikasjon_ekstern.name(),
            Interne
                VarselHendelseEventType.opprettet.name(),
                VarselHendelseEventType.inaktivert.name(),
                VarselHendelseEventType.slettet.name()
            */

    public VarselHendelseMetrikk(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        statuser.forEach(s -> meterRegistry
                .counter(EKSTERN_VARSEL_HENDELSE, VarselHendelseMetrikk.HENDELSE_TYPE, s.name(), FEILMELDING, ""));
    }

    public void incrementInternVarselMetrikk(InternVarselHendelseDTO event) {
        Counter.builder(EKSTERN_VARSEL_HENDELSE)
                .tag(VarselHendelseMetrikk.HENDELSE_TYPE, event.getHendelseType().name())
                .register(meterRegistry)
                .increment();
    }

    public void incrementVarselKvitteringMottatt(EksternVarsling event) {
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
