package no.nav.veilarbaktivitet.stilling_fra_nav;

import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.avro.DelingAvCvRespons;
import no.nav.veilarbaktivitet.avro.TilstandEnum;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringAvroTemplate;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.Arbeidssted;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.KontaktInfo;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.SoftAssertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static no.nav.veilarbaktivitet.util.KafkaTestService.DEFAULT_WAIT_TIMEOUT_DURATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

@Service
public class StillingFraNavTestService {
    @Autowired
    KafkaTestService testService;


    @Value("${topic.inn.stillingFraNav}")
    private String stillingFraNavInnTopic;

    @Value("${topic.ut.stillingFraNav}")
    private String stillingFraNavUtTopic;

    @Autowired
    KafkaStringAvroTemplate<ForesporselOmDelingAvCv> kafkaAvroTemplate;

    public ConsumerRecord<String, DelingAvCvRespons>  opprettStillingFraNav(MockBruker mockBruker, ForesporselOmDelingAvCv melding) {
        assertEquals(mockBruker.getAktorId().get(), melding.getAktorId());

        final Consumer<String, DelingAvCvRespons> consumer = testService.createStringAvroConsumer(stillingFraNavUtTopic);

        String bestillingsId = melding.getBestillingsId();
        kafkaAvroTemplate.send(stillingFraNavInnTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, stillingFraNavUtTopic, DEFAULT_WAIT_TIMEOUT_DURATION);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId().get());
            assertions.assertThat(value.getAktivitetId()).isNotEmpty();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.PROVER_VARSLING);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });
        return record;
    }

    public static ForesporselOmDelingAvCv createForesporselOmDelingAvCv(String bestillingsId, MockBruker mockBruker) {
        return ForesporselOmDelingAvCv.newBuilder()
                .setAktorId(mockBruker.getAktorId().get())
                .setArbeidsgiver("arbeidsgiver")
                .setArbeidssteder(List.of(
                        Arbeidssted.newBuilder()
                                .setAdresse("adresse")
                                .setPostkode("1234")
                                .setKommune("kommune")
                                .setBy("by")
                                .setFylke("fylke")
                                .setLand("land").build(),
                        Arbeidssted.newBuilder()
                                .setAdresse("VillaRosa")
                                .setPostkode(null)
                                .setKommune(null)
                                .setBy(null)
                                .setFylke(null)
                                .setLand("spania").build()))
                .setBestillingsId(bestillingsId)
                .setOpprettet(Instant.now())
                .setOpprettetAv("Z999999")
                .setCallId("callId")
                .setSoknadsfrist("10102021")
                .setStillingsId("stillingsId1234")
                .setStillingstittel("stillingstittel")
                .setSvarfrist(Instant.now().plus(5, ChronoUnit.DAYS))
                .setKontaktInfo(KontaktInfo.newBuilder()
                        .setNavn("Jan Saksbehandler")
                        .setTittel("Nav-ansatt")
                        .setMobil("99999999").build())
                .build();
    }


    public static AktivitetDTO svarPaaDelingAvCv(boolean kanDeleCv, MockBruker mockBruker, MockVeileder veileder, AktivitetDTO aktivitetDTO, Date avtaltDato, int port) {
        DelingAvCvDTO delingAvCvDTO = DelingAvCvDTO.builder()
                .aktivitetVersjon(Long.parseLong(aktivitetDTO.getVersjon()))
                .kanDeles(kanDeleCv)
                .avtaltDato(avtaltDato)
                .build();
        return veileder
                .createRequest()
                .param("aktivitetId", aktivitetDTO.getId())
                .body(delingAvCvDTO)
                .when()
                .put("http://localhost:" + port + "/veilarbaktivitet/api/stillingFraNav/kanDeleCV?fnr=" + mockBruker.getFnr())
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response()
                .as(AktivitetDTO.class);

    }
}
