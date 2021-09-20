package no.nav.veilarbaktivitet.util;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MockBruker {
    private String fnr;
    private String aktorId;
    private boolean underOppfolging;
    private boolean erManuell;
    private boolean erReservertKrr;
    private boolean kanVarsles;
    private boolean erUnderKvp;
    private boolean harBruktNivaa4;

    public static MockBruker happyBruker(String fnr, String aktorId) {
        return MockBruker.builder().fnr(fnr).aktorId(aktorId)
                .underOppfolging(true)
                .erManuell(false)
                .erReservertKrr(false)
                .kanVarsles(true)
                .erUnderKvp(false)
                .harBruktNivaa4(true)
                .build();
    }
}
