package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.aktivitet.domain.EksternAktivitetData;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.AktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.EksternAktivitetskortBestilling;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static no.nav.veilarbaktivitet.util.DateUtils.*;

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

    public static AktivitetData mapTilAktivitetData(AktivitetskortBestilling bestilling, ZonedDateTime opprettetDato) {
        var aktivitetskort = bestilling.getAktivitetskort();
        var eksternAktivitetData = EksternAktivitetData.builder()
                .source(bestilling.getSource())
                .type(bestilling.getAktivitetskortType())
                .tiltaksKode(getTiltakskode(bestilling))
                .detaljer(Optional.ofNullable(aktivitetskort.detaljer).orElse(List.of()))
                .oppgave(aktivitetskort.oppgave)
                .handlinger(Optional.ofNullable(aktivitetskort.handlinger).orElse(List.of()))
                .etiketter(Optional.ofNullable(aktivitetskort.etiketter).orElse(List.of()))
                .build();

        return AktivitetData.builder()
                .funksjonellId(aktivitetskort.id)
                .aktorId(bestilling.getAktorId().get())
                .avtalt(aktivitetskort.avtaltMedNav)
                .tittel(aktivitetskort.tittel)
                .fraDato(toDate(aktivitetskort.startDato))
                .tilDato(toDate(aktivitetskort.sluttDato))
                .beskrivelse(aktivitetskort.beskrivelse)
                .status(aktivitetskort.aktivitetStatus)
                .aktivitetType(AktivitetTypeData.EKSTERNAKTIVITET)
                .lagtInnAv(aktivitetskort.endretAv.identType().mapToInnsenderType())
                .opprettetDato(zonedDateTimeToDate(opprettetDato))
                .endretDato(zonedDateTimeToDate(aktivitetskort.endretTidspunkt))
                .endretAv(aktivitetskort.endretAv.ident())
                .eksternAktivitetData(eksternAktivitetData)
                .build();
    }

}
