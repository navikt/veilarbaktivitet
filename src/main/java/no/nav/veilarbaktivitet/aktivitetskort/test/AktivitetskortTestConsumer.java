package no.nav.veilarbaktivitet.aktivitetskort.test;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetsbestillingCreator;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortType;
import no.nav.veilarbaktivitet.aktivitetskort.MigreringService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AktivitetskortTestConsumer implements TopicConsumer<String, String> {

    private final MigreringService migreringService;

    private final AktivitetsbestillingCreator bestillingsCreator;

    private final AktivitetskortTestDAO aktivitetskortTestDAO;

    @SneakyThrows
    @Override
    public ConsumeStatus consume(ConsumerRecord<String, String> consumerRecord) {
        var bestilling = bestillingsCreator.lagBestilling(consumerRecord);

        if (!bestilling.getAktivitetskortType().equals(AktivitetskortType.ARENA_TILTAK)) {
            return ConsumeStatus.OK;
        }

        var funksjonellId = bestilling.getAktivitetskort().getId();

        boolean harSettAktivitet = aktivitetskortTestDAO.harSettAktivitet(funksjonellId);
        if (harSettAktivitet) {
            return ConsumeStatus.OK;
        } else {
            aktivitetskortTestDAO.lagreFunksjonellId(funksjonellId);
        }

        LocalDateTime opprettetTidspunkt = bestilling.getAktivitetskort().getEndretTidspunkt().toLocalDateTime();

        var oppfolgingsperiode = migreringService.finnOppfolgingsperiode(bestilling.getAktorId(), opprettetTidspunkt);

        if (oppfolgingsperiode.isEmpty()) {
            // metrikk? aktorId, opprettetTidspunkt
            // finn ut hvorfor brukeren ikke har oppfølgingsperioder, hvis få - ignorer i prod?
        }

        return ConsumeStatus.OK;
    }
}
