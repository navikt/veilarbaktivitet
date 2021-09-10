package no.nav.veilarbaktivitet.testutils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;

import static org.junit.Assert.assertEquals;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AktivietAssertUtils {
    public static void assertOpprettetAktivitet(AktivitetDTO expected, AktivitetDTO actual) {
        AktivitetDTO aktivitetDTO = expected.toBuilder()
                // overskriv system-genererte attributter
                .id(actual.getId())
                .versjon(actual.getVersjon())
                .opprettetDato(actual.getOpprettetDato())
                .endretDato(actual.getEndretDato())
                .endretAv(actual.getEndretAv())
                .lagtInnAv(actual.getLagtInnAv())
                .transaksjonsType(actual.getTransaksjonsType())
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
                .lagtInnAv(actual.getLagtInnAv())
                .transaksjonsType(actual.getTransaksjonsType())
                .build();
        // sammenlign resten - forutsetter implementert equals
        assertEquals(aktivitetDTO, actual);
    }
}
