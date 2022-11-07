package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.aktivitet.domain.EksternAktivitetData;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.AktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.EksternAktivitetskortBestilling;

import java.time.LocalDateTime;

import static no.nav.veilarbaktivitet.util.DateUtils.localDateTimeToDate;
import static no.nav.veilarbaktivitet.util.DateUtils.toDate;

public class AktivitetskortMapper {

    private AktivitetskortMapper() {
    }

    private static String getTiltakskode(AktivitetskortBestilling bestilling) {
        if (bestilling instanceof ArenaAktivitetskortBestilling arenaAktivitetskortBestilling) {
            return arenaAktivitetskortBestilling.getArenaTiltakskode();
        } else if (bestilling instanceof EksternAktivitetskortBestilling) {
            return null;
        } else {
            throw new IllegalStateException("Unexpected value: " + bestilling);
        }
    }

    public static AktivitetData mapTilAktivitetData(AktivitetskortBestilling bestilling, LocalDateTime opprettetDato) {
        var aktivitetskort = bestilling.getAktivitetskort();
        EksternAktivitetData eksternAktivitetData = EksternAktivitetData.builder()
                .source(bestilling.getSource())
                .type(bestilling.getAktivitetskortType())
                .tiltaksKode(getTiltakskode(bestilling))
                .detaljer(aktivitetskort.detaljer)
                .oppgave(aktivitetskort.oppgave)
                .handlinger(aktivitetskort.handlinger)
                .etiketter(aktivitetskort.etiketter)
                .build();

        return AktivitetData.builder()
                .funksjonellId(aktivitetskort.id)
                .aktorId(bestilling.getAktorId().get())
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
