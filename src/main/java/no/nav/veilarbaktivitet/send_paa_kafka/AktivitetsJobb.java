package no.nav.veilarbaktivitet.send_paa_kafka;

public interface AktivitetsJobb {
    void behandleAktivitet(long aktivitetId, long aktivitetVersjon);
    JobbType getJobbType();
}
