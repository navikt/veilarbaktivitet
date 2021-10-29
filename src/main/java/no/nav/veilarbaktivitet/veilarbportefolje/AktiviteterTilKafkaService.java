package no.nav.veilarbaktivitet.veilarbportefolje;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AktiviteterTilKafkaService {
    private final KafkaAktivitetDAO dao;
    private final AktivitetKafkaProducerService producerService;

    @Timed
    public void sendOppTil5000AktiviterTilPortefolje() {
        List<KafkaAktivitetMeldingV4> meldinger = dao.hentOppTil5000MeldingerSomIkkeErSendtPaAiven();
        for (KafkaAktivitetMeldingV4 melding : meldinger) {
            producerService.sendAktivitetMelding(melding);
        }
    }

}
