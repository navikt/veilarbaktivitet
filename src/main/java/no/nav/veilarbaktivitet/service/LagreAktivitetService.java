package no.nav.veilarbaktivitet.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import no.nav.common.types.feil.VersjonsKonflikt;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.kafka.KafkaAktivitetMelding;
import no.nav.veilarbaktivitet.kafka.KafkaService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LagreAktivitetService {
    private final AktivitetDAO aktivitetDAO;
    private final KafkaService kafkaService;
    private final MeterRegistry meterRegistry;

    @Transactional
    public void lagreAktivitet(AktivitetData aktivitetData) {
        try {
            meterRegistry.timer("my.timer").record(() -> {
                aktivitetDAO.insertAktivitet(aktivitetData);
                kafkaService.sendMelding(KafkaAktivitetMelding.of(aktivitetData));
                meterRegistry.counter("my.counter").increment();
            });
        } catch (DuplicateKeyException e) {
            throw new VersjonsKonflikt();
        }
    }
}
