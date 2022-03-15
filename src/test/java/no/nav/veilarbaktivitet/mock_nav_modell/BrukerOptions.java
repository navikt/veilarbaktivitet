package no.nav.veilarbaktivitet.mock_nav_modell;

import lombok.Builder;
import lombok.Getter;

@Builder(toBuilder = true)
@Getter
public class BrukerOptions {
    private boolean underOppfolging;
    private boolean erManuell;
    private boolean erReservertKrr;
    private boolean kanVarsles;
    private boolean erUnderKvp;
    private String kontorsperreEnhet;
    private boolean harBruktNivaa4;

    private boolean oppfolgingFeiler;
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
                .harBruktNivaa4(true);
    }

}
