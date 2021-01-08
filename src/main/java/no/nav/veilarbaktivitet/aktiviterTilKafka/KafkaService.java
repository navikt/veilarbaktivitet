package no.nav.veilarbaktivitet.aktiviterTilKafka;

public interface KafkaService {
    long sendMelding(KafkaAktivitetMeldingV3 meldingV3);
}
