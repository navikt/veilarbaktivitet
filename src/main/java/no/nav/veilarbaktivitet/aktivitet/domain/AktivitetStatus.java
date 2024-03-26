package no.nav.veilarbaktivitet.aktivitet.domain;

public enum AktivitetStatus {
    PLANLAGT("Planlagt"),
    GJENNOMFORES("Gjennomføres"),
    FULLFORT("Fullført"),
    BRUKER_ER_INTERESSERT("Bruker er interessert"),
    AVBRUTT("Avbrutt");

    public final String text;

    AktivitetStatus(String text) {
        this.text = text;
    }

}
