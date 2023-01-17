package no.nav.veilarbaktivitet.aktivitetskort.test;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetsbestillingCreator;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortType;
import no.nav.veilarbaktivitet.aktivitetskort.MigreringService;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.AktivitetskortBestilling;
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO;
import no.nav.veilarbaktivitet.person.Person;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AktivitetskortTestConsumer implements TopicConsumer<String, String> {

    private final MigreringService migreringService;

    private final AktivitetsbestillingCreator bestillingsCreator;

    private final AktivitetskortTestDAO aktivitetskortTestDAO;

    private final AktivitetskortTestMetrikker aktivitetskortTestMetrikker;

    @SneakyThrows
    @Override
    public ConsumeStatus consume(ConsumerRecord<String, String> consumerRecord) {
        AktivitetskortBestilling bestilling;

        try {
            bestilling = bestillingsCreator.lagBestilling(consumerRecord);
        } catch (Exception e) {
            aktivitetskortTestMetrikker.countError(e);
            log.warn("MIGRERINGSERVICE.FINNOPPFOLGINGSPERIODE - feil i behandling av innkommende melding. Ignorerer", e);
            return ConsumeStatus.OK;
        }

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
        LocalDate startDato = bestilling.getAktivitetskort().getStartDato();
        LocalDate sluttDato = bestilling.getAktivitetskort().getSluttDato();
        Person.AktorId aktorId = bestilling.getAktorId();

        Optional<OppfolgingPeriodeMinimalDTO> oppfolgingsperiode = migreringService.finnOppfolgingsperiode(aktorId, opprettetTidspunkt, startDato, sluttDato);

        if (oppfolgingsperiode.isEmpty()) {
            aktivitetskortTestMetrikker.countFinnOppfolgingsperiode(5);

            log.info("MIGRERINGSERVICE.FINNOPPFOLGINGSPERIODE case 5 (bruker har ingen oppfølgingsperioder / periode for langt unna opprettetTidspunkt) - aktorId={}, opprettetTidspunkt={}, startDato={}, sluttDato={}, oppfolgingsperioder={}",
                    aktorId.get(),
                    opprettetTidspunkt,
                    startDato,
                    sluttDato,
                    List.of());
        }

        return ConsumeStatus.OK;
    }
}
