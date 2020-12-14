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
    public void sendOppTil1000AktiviterPaaKafka() {
        List<KafkaAktivitetMeldingV2> meldinger = dao.hentOppTil1000MeldingerUtenKafka();
        for (KafkaAktivitetMeldingV2 melding : meldinger) {
            trySendMelding(melding);
        }
    }

    private void trySendMelding(KafkaAktivitetMeldingV2 melding) {
        try {
            sendMelding(melding);
        } catch (Exception e) {
            log.error("feilet ved sending av melding til kafka", e);
        }
    }

    private void sendMelding(KafkaAktivitetMeldingV2 melding) {
        registry.timer("send.aktivitet.paaa.kafka").record(() -> {
            kafka.sendMeldingV2(melding);
            dao.insertMeldingSendtPaaKafka(melding);
        });
    }
}
