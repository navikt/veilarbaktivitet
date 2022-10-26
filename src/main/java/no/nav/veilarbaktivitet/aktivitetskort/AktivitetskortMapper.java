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

    static AktivitetData mapTilAktivitetData(AktivitetskortDTO aktivitetskortDTO, LocalDateTime opprettetDato, String aktorId) {
        EksternAktivitetData eksternAktivitetData = EksternAktivitetData.builder()
                .source(null)
                .type(null)
                .tiltaksKode(null)
                .detaljer(aktivitetskortDTO.detaljer)
                .oppgave(aktivitetskortDTO.oppgave)
                .handlinger(aktivitetskortDTO.handlinger)
                .etiketter(aktivitetskortDTO.etiketter)
                .build();

        return AktivitetData.builder()
                .funksjonellId(aktivitetskortDTO.id)
                .aktorId(aktorId)
                .tittel(aktivitetskortDTO.tittel)
                .fraDato(toDate(aktivitetskortDTO.startDato))
                .tilDato(toDate(aktivitetskortDTO.sluttDato))
                .beskrivelse(aktivitetskortDTO.beskrivelse)
                .status(aktivitetskortDTO.aktivitetStatus)
                .aktivitetType(AktivitetTypeData.EKSTERNAKTIVITET)
                .lagtInnAv(aktivitetskortDTO.endretAv.identType().mapToInnsenderType())
                .opprettetDato(localDateTimeToDate(opprettetDato))
                .endretDato(localDateTimeToDate(aktivitetskortDTO.endretTidspunkt))
                .endretAv(aktivitetskortDTO.endretAv.ident())
                .eksternAktivitetData(eksternAktivitetData)
                .build();
    }

}
