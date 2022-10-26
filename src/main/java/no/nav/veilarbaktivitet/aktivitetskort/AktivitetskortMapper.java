package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.aktivitet.domain.EksternAktivitetData;

import java.time.LocalDateTime;

import static no.nav.veilarbaktivitet.util.DateUtils.localDateTimeToDate;
import static no.nav.veilarbaktivitet.util.DateUtils.toDate;

public class AktivitetskortMapper {

    private AktivitetskortMapper() {
    }

    static AktivitetData mapTilAktivitetData(Aktivitetskort aktivitetskort, MeldingContext meldingContext, LocalDateTime opprettetDato, String aktorId) {
        EksternAktivitetData eksternAktivitetData = EksternAktivitetData.builder()
                .source(meldingContext.source())
                .type(meldingContext.aktivitetskortType())
                .tiltaksKode(meldingContext.arenaTiltakskode())
                .detaljer(aktivitetskort.detaljer)
                .oppgave(aktivitetskort.oppgave)
                .handlinger(aktivitetskort.handlinger)
                .etiketter(aktivitetskort.etiketter)
                .build();

        return AktivitetData.builder()
                .funksjonellId(aktivitetskort.id)
                .aktorId(aktorId)
                .tittel(aktivitetskort.tittel)
                .fraDato(toDate(aktivitetskort.startDato))
                .tilDato(toDate(aktivitetskort.sluttDato))
                .beskrivelse(aktivitetskort.beskrivelse)
                .status(aktivitetskort.aktivitetStatus)
                .aktivitetType(AktivitetTypeData.EKSTERNAKTIVITET)
                .lagtInnAv(aktivitetskort.endretAv.identType().mapToInnsenderType())
                .opprettetDato(localDateTimeToDate(opprettetDato))
                .endretDato(localDateTimeToDate(aktivitetskort.endretTidspunkt))
                .endretAv(aktivitetskort.endretAv.ident())
                .eksternAktivitetData(eksternAktivitetData)
                .build();
    }

}
