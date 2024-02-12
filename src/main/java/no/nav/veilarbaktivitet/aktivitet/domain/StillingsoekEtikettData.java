package no.nav.veilarbaktivitet.aktivitet.domain;

public enum StillingsoekEtikettData {
    SOKNAD_SENDT("Sendt søknad og venter på svar"),
    INNKALT_TIL_INTERVJU("Ikke fått jobben"),
    AVSLAG("Fått jobbtilbud \uD83C\uDF89"),
    JOBBTILBUD("Fått jobben");

    public final String text;

    StillingsoekEtikettData(String text) {
        this.text = text;
    }
}
