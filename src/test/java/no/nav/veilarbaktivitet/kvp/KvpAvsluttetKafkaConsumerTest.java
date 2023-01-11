package no.nav.veilarbaktivitet.kvp;


import no.nav.common.featuretoggle.UnleashClient;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortType;
import no.nav.veilarbaktivitet.aktivitetskort.MigreringService;
import no.nav.veilarbaktivitet.config.kafka.NavCommonKafkaConfig;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaJsonTemplate;
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder;
import no.nav.veilarbaktivitet.testutils.AktivitetskortTestBuilder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.when;

public class KvpAvsluttetKafkaConsumerTest extends SpringBootTestBase {

    MockBruker mockBruker;
    MockVeileder mockVeileder;

    @Autowired
    KafkaJsonTemplate<KvpAvsluttetKafkaDTO> navCommonKafkaJsonTemplate;

    @Autowired
    UnleashClient unleashClient;

    @Value("${topic.inn.kvpAvsluttet}")
    String kvpAvsluttetTopic;

    @BeforeEach
    public void before() {
        var medKvp = BrukerOptions.happyBrukerBuilder().erUnderKvp(true).kontorsperreEnhet("123").build();
        mockBruker = MockNavService.createBruker(medKvp);
        mockVeileder = MockNavService.createVeileder(mockBruker);
        when(unleashClient.isEnabled(MigreringService.EKSTERN_AKTIVITET_TOGGLE)).thenReturn(true);
    }

    @Test
    public void skal_avbryte_aktiviteter_i_kvp_periode() throws ExecutionException, InterruptedException, TimeoutException {
        var aktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN);
        var opprettetAktivitet = aktivitetTestService.opprettAktivitet(mockBruker, aktivitet);
        Assertions.assertThat(opprettetAktivitet.getStatus()).isEqualTo(AktivitetStatus.PLANLAGT);

        var kvpAvsluttetDato = ZonedDateTime.now();

        var kvpAvsluttet =
                new KvpAvsluttetKafkaDTO()
                        .setAktorId(mockBruker.getAktorId())
                        .setAvsluttetAv(mockVeileder.getNavIdent())
                        .setAvsluttetBegrunnelse("Derfor")
                        .setAvsluttetDato(kvpAvsluttetDato);

        var sendResultListenableFuture = navCommonKafkaJsonTemplate.send(kvpAvsluttetTopic, kvpAvsluttet);
        long offset = sendResultListenableFuture.get(2, TimeUnit.SECONDS).getRecordMetadata().offset();
        System.out.println("Ho");
        kafkaTestService.assertErKonsumertAiven(kvpAvsluttetTopic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, offset, 2);
        var avsluttetAktivitet = aktivitetTestService.hentAktivitet(mockBruker, opprettetAktivitet.getId());

        Assertions.assertThat(avsluttetAktivitet.getStatus()).isEqualTo(AktivitetStatus.AVBRUTT);
    }

    @Test
    public void skal_ikke_avbryte_eksterne_aktiviteter_i_kvp_periode() throws ExecutionException, InterruptedException, TimeoutException {

        var aktivitetskort = AktivitetskortTestBuilder.ny(UUID.randomUUID(), AktivitetStatus.PLANLAGT, ZonedDateTime.now(), mockBruker);
        var kafkaAktivitetskortWrapperDTO = AktivitetskortTestBuilder.aktivitetskortMelding(
                aktivitetskort, UUID.randomUUID(), "TEAM_TILTAK", AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD);

        aktivitetTestService.opprettEksterntAktivitetsKort(List.of(kafkaAktivitetskortWrapperDTO));

        var opprettetAktivitet = aktivitetTestService.hentAktivitetByFunksjonellId(mockBruker, mockVeileder, aktivitetskort.getId());
        Assertions.assertThat(opprettetAktivitet.getStatus()).isEqualTo(AktivitetStatus.PLANLAGT);

        ZonedDateTime kvpAvsluttetDato = ZonedDateTime.now();

        KvpAvsluttetKafkaDTO kvpAvsluttet =
                new KvpAvsluttetKafkaDTO()
                        .setAktorId(mockBruker.getAktorId())
                        .setAvsluttetAv(mockVeileder.getNavIdent())
                        .setAvsluttetBegrunnelse("Derfor")
                        .setAvsluttetDato(kvpAvsluttetDato);

        ListenableFuture<SendResult<String, KvpAvsluttetKafkaDTO>> sendResultListenableFuture = navCommonKafkaJsonTemplate.send(kvpAvsluttetTopic, kvpAvsluttet);
        long offset = sendResultListenableFuture.get(2, TimeUnit.SECONDS).getRecordMetadata().offset();
        kafkaTestService.assertErKonsumertAiven(kvpAvsluttetTopic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, offset, 2);
        AktivitetDTO avsluttetAktivitet = aktivitetTestService.hentAktivitet(mockBruker, opprettetAktivitet.getId());

        Assertions.assertThat(avsluttetAktivitet.getStatus()).isEqualTo(AktivitetStatus.PLANLAGT);
    }

}