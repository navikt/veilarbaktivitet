package no.nav.veilarbaktivitet.aktiviterTilKafka;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AktiviteterTilKafkaService {
    private final KafkaAktivitetDAO dao;
    private final KafkaService kafka;
    private final MeterRegistry registry;

    @Timed
    public void sendOppTil5000AktiviterPaaKafka() {
        List<KafkaAktivitetMeldingV3> meldinger = dao.hentOppTil5000MeldingerUtenKafka();
        for (KafkaAktivitetMeldingV3 melding : meldinger) {
            trySendMelding(melding);
        }
    }

    private void trySendMelding(KafkaAktivitetMeldingV3 melding) {
        try {
            sendMelding(melding);
        } catch (Exception e) {
            log.error("feilet ved sending av melding for aktivitet ID: " + melding.getAktivitetId() + " version: " + melding.getVersion() + " til kafka", e);
        }
    }

    private void sendMelding(KafkaAktivitetMeldingV3 melding) {
        registry.timer("send.aktivitet.paaa.kafka").record(() -> {
            long offset = kafka.sendMeldingV3(melding);
            dao.insertMeldingSendtPaaKafka(melding, offset);
        });
    }
}
