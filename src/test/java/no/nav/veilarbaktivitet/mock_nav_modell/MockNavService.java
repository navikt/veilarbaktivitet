package no.nav.veilarbaktivitet.mock_nav_modell;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MockNavService {
    private static final Map<String, MockBruker> fnrBruker = new HashMap<>();
    private static final Map<String, MockBruker> aktorIdBruker = new HashMap<>();
    private static final Map<String, MockVeileder> veleder = new HashMap<>();

    public static MockBruker createHappyBruker() {
        return createBruker(BrukerOptions.happyBruker());
    }

    public static MockBruker createBruker(BrukerOptions brukerOptions) {
        String fnr = generateFnr();
        String aktorId = aktorIdFromFnr(aktorIdFromFnr(fnr));
        MockBruker mockBruker = new MockBruker(fnr, aktorId, brukerOptions);
        fnrBruker.put(fnr, mockBruker);
        aktorIdBruker.put(aktorId, mockBruker);
        WireMockUtil.stubBruker(mockBruker);
        return mockBruker;
    }

    public static void updateBruker(MockBruker mockBruker, BrukerOptions brukerOptions) {
        mockBruker.setBrukerOptions(brukerOptions);
        WireMockUtil.stubBruker(mockBruker);
    }

    public static MockVeileder createVeileder(MockBruker... mockBruker) {
        MockVeileder veileder = createVeileder();
        for (MockBruker bruker : mockBruker) {
            veileder.addBruker(bruker);
        }
        return veileder;
    }

    public static MockVeileder createVeileder() {
        MockVeileder mockVeileder = new MockVeileder(genereteVeilederIdent());
        veleder.put(mockVeileder.getNavIdent(), mockVeileder);
        return mockVeileder;
    }

    private static String generateEnhet() {
        return null; //TODO fiks
    }

    private static String genereteVeilederIdent() {
        Random random = new Random();
        char letter = (char) ('A' + random.nextInt(26));
        String numbers = IntStream.range(0, 6)
                .map(i -> random.nextInt(9))
                .mapToObj(Integer::toString)
                .collect(Collectors.joining());
        return letter + numbers;
    }

    public static String generateFnr() {
        //TODO fiks se forskjell på aktorid og fnr fra nummer
        return IntStream.range(0, 11)
                .map(i -> new Random().nextInt(9))
                .mapToObj(Integer::toString)
                .collect(Collectors.joining());
    }

    private static String aktorIdFromFnr(String fnr) {
        return new StringBuilder(fnr).reverse().toString();
    }


    static MockVeileder getVeileder(String id) {
        return veleder.get(id);
    }

    static MockBruker getBruker(String ident) {
        MockBruker mockBruker = fnrBruker.get(ident);
        if (mockBruker != null) {
            return mockBruker;
        }
        return aktorIdBruker.get(ident);
    }
}
