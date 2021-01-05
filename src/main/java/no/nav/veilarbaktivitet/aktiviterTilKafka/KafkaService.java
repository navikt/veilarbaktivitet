package no.nav.veilarbaktivitet.aktiviterTilKafka;

public interface KafkaService {
    void sendMelding(KafkaAktivitetMelding melding);

    long sendMeldingV3(KafkaAktivitetMeldingV3 meldingV3);
}
