package no.nav.veilarbaktivitet.aktiviterTilKafka;

import io.micrometer.core.annotation.Timed;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class AktiviteterTilKafkaService {
    private final KafkaAktivitetDAO dao;
    private final KafkaService kafka;

    @Timed
    public void sendOppTil1000AktiviterPaaKafka() {
        List<KafkaAktivitetMeldingV2> meldinger = dao.hentOppTil1000MeldingerUtenKafka();
        for (KafkaAktivitetMeldingV2 melding : meldinger) {
            kafka.sendMeldingV2(melding);
            dao.insertMeldingSendtPaaKafka(melding);
        }
    }
}
