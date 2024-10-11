package no.nav.veilarbaktivitet.kvp;


import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortUtil;
import no.nav.veilarbaktivitet.aktivitetskort.MigreringService;
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortStatus;
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType;
import no.nav.veilarbaktivitet.aktivitetskort.dto.KafkaAktivitetskortWrapperDTO;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.MessageSource;
import no.nav.veilarbaktivitet.config.kafka.NavCommonKafkaConfig;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaJsonTemplate;
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.SendResult;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.when;
class KvpAvsluttetKafkaConsumerTest extends SpringBootTestBase {

    MockBruker mockBruker;
    MockVeileder mockVeileder;

    @Autowired
    KafkaJsonTemplate<KvpAvsluttetKafkaDTO> navCommonKafkaJsonTemplate;

    @Value("${topic.inn.kvpAvsluttet}")
    String kvpAvsluttetTopic;

    @BeforeEach
    public void before() {
        var medKvp = BrukerOptions.happyBrukerBuilder().erUnderKvp(true).build();
        mockBruker = navMockService.createBruker(medKvp);
        mockVeileder =  navMockService.createVeileder(mockBruker);
        when(unleash.isEnabled(MigreringService.VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE)).thenReturn(true);
    }

    @Test
    void skal_avbryte_aktiviteter_i_kvp_periode() throws ExecutionException, InterruptedException, TimeoutException {
        var aktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN);
        var opprettetAktivitet = aktivitetTestService.opprettAktivitet(mockBruker, aktivitet);
        Assertions.assertThat(opprettetAktivitet.getStatus()).isEqualTo(AktivitetStatus.PLANLAGT);

        var kvpAvsluttetDato = ZonedDateTime.now();

        var kvpAvsluttet =
                new KvpAvsluttetKafkaDTO()
                        .setAktorId(mockBruker.getAktorId().get())
                        .setAvsluttetAv(mockVeileder.getNavIdent())
                        .setAvsluttetBegrunnelse("Derfor")
                        .setAvsluttetDato(kvpAvsluttetDato);

        var sendResultListenableFuture = navCommonKafkaJsonTemplate.send(kvpAvsluttetTopic, kvpAvsluttet);
        long offset = sendResultListenableFuture.get(2, TimeUnit.SECONDS).getRecordMetadata().offset();
        System.out.println("Ho");
        kafkaTestService.assertErKonsumert(kvpAvsluttetTopic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, offset);
        var avsluttetAktivitet = aktivitetTestService.hentAktivitet(mockBruker, opprettetAktivitet.getId());

        Assertions.assertThat(avsluttetAktivitet.getStatus()).isEqualTo(AktivitetStatus.AVBRUTT);
    }

    @Test
    void skal_ikke_avbryte_eksterne_aktiviteter_i_kvp_periode() throws ExecutionException, InterruptedException, TimeoutException {

        var aktivitetskort = AktivitetskortUtil.ny(UUID.randomUUID(), AktivitetskortStatus.PLANLAGT, ZonedDateTime.now(), mockBruker);
        var kafkaAktivitetskortWrapperDTO = new KafkaAktivitetskortWrapperDTO(
                aktivitetskort, UUID.randomUUID(), AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD, MessageSource.TEAM_TILTAK);

        aktivitetTestService.opprettEksterntAktivitetsKort(List.of(kafkaAktivitetskortWrapperDTO));

        var opprettetAktivitet = aktivitetTestService.hentAktivitetByFunksjonellId(mockBruker, mockVeileder, aktivitetskort.getId());
        Assertions.assertThat(opprettetAktivitet.getStatus()).isEqualTo(AktivitetStatus.PLANLAGT);

        ZonedDateTime kvpAvsluttetDato = ZonedDateTime.now();

        KvpAvsluttetKafkaDTO kvpAvsluttet =
                new KvpAvsluttetKafkaDTO()
                        .setAktorId(mockBruker.getAktorId().get())
                        .setAvsluttetAv(mockVeileder.getNavIdent())
                        .setAvsluttetBegrunnelse("Derfor")
                        .setAvsluttetDato(kvpAvsluttetDato);

        CompletableFuture<SendResult<String, KvpAvsluttetKafkaDTO>> sendResultListenableFuture = navCommonKafkaJsonTemplate.send(kvpAvsluttetTopic, kvpAvsluttet);
        long offset = sendResultListenableFuture.get(2, TimeUnit.SECONDS).getRecordMetadata().offset();
        kafkaTestService.assertErKonsumert(kvpAvsluttetTopic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, offset);
        AktivitetDTO avsluttetAktivitet = aktivitetTestService.hentAktivitet(mockBruker, opprettetAktivitet.getId());

        Assertions.assertThat(avsluttetAktivitet.getStatus()).isEqualTo(AktivitetStatus.PLANLAGT);
    }

}
