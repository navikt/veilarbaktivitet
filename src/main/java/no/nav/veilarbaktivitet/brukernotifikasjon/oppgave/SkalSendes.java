package no.nav.veilarbaktivitet.brukernotifikasjon.oppgave;

import lombok.Getter;
import no.nav.veilarbaktivitet.domain.AktivitetStatus;

import java.sql.Date;

@Getter
class SkalSendes {
    private final long id;
    private final String brukernotifikasjonId;
    private final long aktivitetId;
    private final String melding;
    private final String oppfolgingsperiode;
    private final String aktorId;
    private final AktivitetStatus livslopstatusKode;
    private final Date historiskDato;

    SkalSendes(long id, String brukernotifikasjonId, long aktivitetId, String melding, String oppfolgingsperiode, String aktorId, String livslopstatusKode, Date historiskDato) {
        this.id = id;
        this.brukernotifikasjonId = brukernotifikasjonId;
        this.aktivitetId = aktivitetId;
        this.melding = melding;
        this.oppfolgingsperiode = oppfolgingsperiode;
        this.aktorId = aktorId;
        this.livslopstatusKode = AktivitetStatus.valueOf(livslopstatusKode);
        this.historiskDato = historiskDato;
    }

    public boolean skalAbrytes() {
        return historiskDato == null || AktivitetStatus.AVBRUTT.equals(livslopstatusKode) || AktivitetStatus.FULLFORT.equals(livslopstatusKode);
    }
}
