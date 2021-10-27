package no.nav.veilarbaktivitet.veilarbportefolje;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.config.kafka.KafkaJsonTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AktivitetTilKakfak {
    private final AktivitetService aktivitetService;
    private final KafkaJsonTemplate<String, AktivitetData> template;

    @Value("${topic.ut.aktivitetdata.rawjson}")
    private String toppic;

    @SneakyThrows
    void sendVersionTilKafka(long version) {
        AktivitetData aktivitetData = aktivitetService.hentAktivitetMedFHOForVersion(version);

        template.send(toppic, aktivitetData.getId().toString(), aktivitetData).get();
    }

}
