package no.nav.veilarbaktivitet.veilarbportefolje;

public enum CvKanDelesStatus {
    JA,
    NEI,
    IKKE_SVART;

    public static CvKanDelesStatus valueOf(Integer cvKanDeles){
        return switch (cvKanDeles) {
            case 1 -> JA;
            case 0 -> NEI;
            default -> IKKE_SVART;
        };
    }
}
