package no.nav.veilarbaktivitet.service;

import lombok.AllArgsConstructor;
import no.nav.common.types.feil.VersjonsKonflikt;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktiviterTilKafka.KafkaAktivitetMelding;
import no.nav.veilarbaktivitet.aktiviterTilKafka.KafkaService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class LagreAktivitetService {
    private final AktivitetDAO aktivitetDAO;
    private final KafkaService kafkaService;

    @Transactional
    public void lagreAktivitet(AktivitetData aktivitetData) {
        lagreAktivitet(aktivitetData, false);
    }

    @Transactional
    public void lagreAktivitet(AktivitetData aktivitetData, boolean nyAktivitet) {
        try {
            aktivitetDAO.insertAktivitet(aktivitetData);
            kafkaService.sendMelding(KafkaAktivitetMelding.of(aktivitetData));
        } catch (DuplicateKeyException e) {
            throw new VersjonsKonflikt();
        }
    }
}
