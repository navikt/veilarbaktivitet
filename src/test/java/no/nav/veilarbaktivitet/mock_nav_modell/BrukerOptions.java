package no.nav.veilarbaktivitet.mock_nav_modell;

import lombok.Builder;
import lombok.Getter;
import no.nav.veilarbaktivitet.person.Navn;

@Builder(toBuilder = true)
@Getter
public class BrukerOptions {
    private boolean underOppfolging;
    private boolean erManuell;
    private boolean erReservertKrr;
    private boolean kanVarsles;
    private boolean erUnderKvp;

    private boolean oppfolgingFeiler;
    private Navn navn;
    private Long sakId;
    private String mål;
    private String fnr;

    /*
    @TODO
    private boolean manuellFeiler;
    private boolean krrFeiler;
    private boolean kvpFeiler;
     */

    public static BrukerOptions happyBruker() {
        return happyBrukerBuilder()
                .build();
    }

    public static BrukerOptionsBuilder happyBrukerBuilder() {
        return BrukerOptions.builder()
                .underOppfolging(true)
                .erManuell(false)
                .erReservertKrr(false)
                .kanVarsles(true)
                .erUnderKvp(false)
                .navn(new Navn("Navn", null, "Navnesen"))
                .sakId(1000L)
                .mål("Å få meg jobb")
                .fnr("01015450300");

    }
}
