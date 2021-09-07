package no.nav.veilarbaktivitet.util;

import lombok.Builder;
import lombok.Data;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    public static MockBruker happyBruker() {
        String fnr = generateFnr();
        String aktorId = aktorIdFromFnr(fnr);
        return MockBruker.builder().fnr(fnr).aktorId(aktorId)
                .underOppfolging(true)
                .erManuell(false)
                .erReservertKrr(false)
                .kanVarsles(true)
                .erUnderKvp(false)
                .harBruktNivaa4(true)
                .build();
    }

    public static String generateFnr() {
        return IntStream.range(0, 11)
                        .map(i -> new Random().nextInt(9))
                        .mapToObj(Integer::toString)
                        .collect(Collectors.joining());
    }

    private static String aktorIdFromFnr(String fnr) {
        return new StringBuilder(fnr).reverse().toString();
    }
}
