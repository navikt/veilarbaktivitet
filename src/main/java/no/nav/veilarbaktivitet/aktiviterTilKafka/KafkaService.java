package no.nav.veilarbaktivitet.aktiviterTilKafka;

public interface KafkaService {
    void sendMelding(KafkaAktivitetMelding melding);

    void sendMeldingV2(KafkaAktivitetMeldingV2 meldingV2);
}
