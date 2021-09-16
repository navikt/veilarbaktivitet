package no.nav.veilarbaktivitet.brukernotifikasjon.oppgave;

import lombok.Builder;
import lombok.Getter;
import no.nav.veilarbaktivitet.domain.AktivitetStatus;

import java.util.Date;

@Getter
@Builder
class SkalSendes {
    private final long id;
    private final String brukernotifikasjonId;
    private final long aktivitetId;
    private final String melding;
    private final String oppfolgingsperiode;
    private final String aktorId;
    private final AktivitetStatus livslopstatusKode;
    private final Date historiskDato;

    public boolean skalAbrytes() {
        return historiskDato != null || AktivitetStatus.AVBRUTT.equals(livslopstatusKode) || AktivitetStatus.FULLFORT.equals(livslopstatusKode);
    }
}
