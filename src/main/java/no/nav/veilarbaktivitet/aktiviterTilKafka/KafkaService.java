package no.nav.veilarbaktivitet.aktiviterTilKafka;

public interface KafkaService {
    long sendMelding(KafkaAktivitetMeldingV3 meldingV3);

    long sendMeldingV4(KafkaAktivitetMeldingV4 meldingV4);
}
