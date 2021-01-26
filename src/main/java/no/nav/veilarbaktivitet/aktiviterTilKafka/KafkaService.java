package no.nav.veilarbaktivitet.aktiviterTilKafka;

public interface KafkaService {
    long sendMeldingV4(KafkaAktivitetMeldingV4 meldingV4);
}
