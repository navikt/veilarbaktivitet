package no.nav.veilarbaktivitet.veilarbportefolje.gammel;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.veilarbportefolje.KafkaAktivitetDAO;
import no.nav.veilarbaktivitet.veilarbportefolje.KafkaAktivitetMeldingV4;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AktiviteterTilPortefoljeService {
    private final KafkaAktivitetDAO dao;
    private final KafkaProducerService producerService;
    private final MeterRegistry registry;

    @Timed
    public void sendOppTil5000AktiviterTilPortefolje() {
        List<KafkaAktivitetMeldingV4> meldinger = dao.hentOppTil5000MeldingerSomIkkeErSendt();
        for (KafkaAktivitetMeldingV4 melding : meldinger) {
            trySendMelding(melding);
        }
    }

    private void trySendMelding(KafkaAktivitetMeldingV4 melding) {
        try {
            sendMeldingV4(melding);
        } catch (Exception e) {
            log.error("feilet ved sending av melding for aktivitet ID: " + melding.getAktivitetId() + " version: " + melding.getVersion() + " til kafka V4", e);
        }
    }

    private void sendMeldingV4(KafkaAktivitetMeldingV4 melding) {
        registry.timer("send_aktivitet_paaa_kafka").record(() -> {
            long offset = producerService.sendAktivitetMelding(melding);
            dao.updateSendtPaKafka(melding.getVersion(), offset);
        });
    }
}
