package no.nav.veilarbaktivitet.aktiviterTilKafka;

public interface KafkaService {
    long sendMelding(KafkaAktivitetMeldingV4 meldingV4);
}
