package no.nav.veilarbaktivitet.kafka;

public interface KafkaService {
    void sendMelding(KafkaAktivitetMelding melding);
}
