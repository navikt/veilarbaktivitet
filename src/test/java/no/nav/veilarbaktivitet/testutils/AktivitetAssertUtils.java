package no.nav.veilarbaktivitet.testutils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AktivitetAssertUtils {
    public static void assertOpprettetAktivitet(AktivitetDTO expected, AktivitetDTO actual) {
        AktivitetDTO aktivitetDTO = expected.toBuilder()
                // overskriv system-genererte attributter
                .id(actual.getId())
                .versjon(actual.getVersjon())
                .opprettetDato(actual.getOpprettetDato())
                .endretDato(actual.getEndretDato())
                .endretAv(actual.getEndretAv())
                .endretAvType(actual.getEndretAvType())
                .transaksjonsType(actual.getTransaksjonsType())
                .oppfolgingsperiodeId(actual.getOppfolgingsperiodeId())
                .forhaandsorientering(actual.getForhaandsorientering())
                .build();
        // sammenlign resten - forutsetter implementert equals
        assertEquals(aktivitetDTO, actual);
    }

    public static void assertOppdatertAktivitet(AktivitetDTO expected, AktivitetDTO actual) {
        AktivitetDTO aktivitetDTO = expected.toBuilder()
                // overskriv system-genererte attributter
                .versjon(actual.getVersjon())
                .endretDato(actual.getEndretDato())
                .endretAv(actual.getEndretAv())
                .endretAvType(actual.getEndretAvType())
                .transaksjonsType(actual.getTransaksjonsType())
                .build();
        // sammenlign resten - forutsetter implementert equals
        assertEquals(aktivitetDTO, actual);
    }
}
