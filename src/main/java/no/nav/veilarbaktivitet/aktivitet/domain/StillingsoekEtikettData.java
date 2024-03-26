package no.nav.veilarbaktivitet.aktivitet.domain;


public enum StillingsoekEtikettData {
    SOKNAD_SENDT("Søknaden er sendt"),
    INNKALT_TIL_INTERVJU("Skal på intervju"),
    AVSLAG("Ikke fått jobben"),
    JOBBTILBUD("Fått jobbtilbud");

    public final String text;

    StillingsoekEtikettData(String text) {
        this.text = text;
    }
}
