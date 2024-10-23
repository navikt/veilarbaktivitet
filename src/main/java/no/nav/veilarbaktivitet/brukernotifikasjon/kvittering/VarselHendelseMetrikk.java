package no.nav.veilarbaktivitet.brukernotifikasjon.kvittering;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import kotlin.enums.EnumEntries;
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.EksternVarsling;
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.Feilet;
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.InternVarselHendelseDTO;
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.VarselHendelseEventType;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class VarselHendelseMetrikk {
    private final MeterRegistry meterRegistry;
    public static final String VARSEL_HENDELSE = "varsel_hendelse";
    private static final String HENDELSE_TYPE = "hendelse_type";
    private static final String VARSEL_TYPE = "varsel_type";
    private static final String FEILMELDING = "feilmelding";
    private static final String TID_TIL_INAKTIVERT = "tid_til_inaktivert";
    private static final EnumEntries<VarselHendelseEventType> statuser = VarselHendelseEventType.getEntries();
    private Timer tidTilInaktiveringTimer;
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

        statuser.forEach(hendelseType -> {
            if (hendelseType == VarselHendelseEventType.feilet_ekstern) {
                meterRegistry.counter(VARSEL_HENDELSE, HENDELSE_TYPE, hendelseType.name(), VARSEL_TYPE, "",  FEILMELDING, "");
            } else {
                meterRegistry.counter(VARSEL_HENDELSE, HENDELSE_TYPE, hendelseType.name(), VARSEL_TYPE, "");
            }
        });
        tidTilInaktiveringTimer = Timer.builder(TID_TIL_INAKTIVERT)
                .description("Tid fra varsel opprettet til inaktivering")
                .register(meterRegistry);
    }

    public void recordTidTilInaktivering(Duration duration) {
        tidTilInaktiveringTimer.record(duration);
    }

    public void incrementInternVarselMetrikk(InternVarselHendelseDTO event) {
        Counter.builder(VARSEL_HENDELSE)
                .tag(VarselHendelseMetrikk.HENDELSE_TYPE, event.getHendelseType().name())
                .tag(VarselHendelseMetrikk.VARSEL_TYPE, event.getVarseltype().name())
                .register(meterRegistry)
                .increment();
    }

    public void incrementVarselKvitteringMottatt(EksternVarsling event) {
        var counterBuilder = Counter.builder(VARSEL_HENDELSE);
        if(event.getHendelseType() == VarselHendelseEventType.feilet_ekstern) {
            counterBuilder.tag(FEILMELDING, ((Feilet) event).getFeilmelding() );
        }
        counterBuilder
                .tag(VarselHendelseMetrikk.HENDELSE_TYPE, event.getHendelseType().name())
                .tag(VarselHendelseMetrikk.VARSEL_TYPE, event.getVarseltype().name())
                .register(meterRegistry)
                .increment();
    }
}
