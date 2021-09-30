package no.nav.veilarbaktivitet.util;

import io.restassured.response.Response;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.avro.DelingAvCvRespons;
import no.nav.veilarbaktivitet.avro.TilstandEnum;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.domain.AktivitetsplanDTO;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.RestassuredUser;
import no.nav.veilarbaktivitet.stilling_fra_nav.KontaktpersonData;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.Arbeidssted;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.KontaktInfo;
import no.nav.veilarbaktivitet.testutils.AktivitetAssertUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

@Service
public class AktivitetTestService {

    @Autowired
    KafkaTestService testService;

    @Autowired
    AktivitetTestService testAktivitetService;

    @Value("${topic.inn.stillingFraNav}")
    private String stillingFraNavInnTopic;

    @Value("${topic.ut.stillingFraNav}")
    private String stillingFraNavUtTopic;

    @Autowired
    KafkaTemplate<String, ForesporselOmDelingAvCv> producer;

    public AktivitetDTO opprettStillingFraNav(MockBruker mockBruker, int springPort) {
        return opprettStillingFraNav(mockBruker, createMelding(UUID.randomUUID().toString(), mockBruker), springPort);
    }

    public AktivitetDTO opprettStillingFraNav(MockBruker mockBruker, ForesporselOmDelingAvCv melding, int springPort) {
        assertEquals(mockBruker.getAktorId(), melding.getAktorId());

        final Consumer<String, DelingAvCvRespons> consumer = testService.createStringAvroConsumer(stillingFraNavUtTopic);

        String bestillingsId = melding.getBestillingsId();
        producer.send(stillingFraNavInnTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, stillingFraNavUtTopic, 5000);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId());
            assertions.assertThat(value.getAktivitetId()).isNotEmpty();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.PROVER_VARSLING);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });

        AktivitetsplanDTO aktivitetsplanDTO = testAktivitetService.hentAktiviteterForFnr(springPort, mockBruker);
        AktivitetDTO aktivitetDTO = aktivitetsplanDTO.getAktiviteter().stream().filter(a -> a.getId().equals(value.getAktivitetId())).findAny().get();

        //TODO skriv bedre test
        assertEquals(AktivitetTypeDTO.STILLING_FRA_NAV, aktivitetDTO.getType());
        assertEquals(melding.getStillingstittel(), aktivitetDTO.getTittel());
        assertEquals("/rekrutteringsbistand/" + melding.getStillingsId(), aktivitetDTO.getLenke());
        assertEquals(melding.getBestillingsId(), aktivitetDTO.getStillingFraNavData().getBestillingsId());

        KontaktInfo meldingKontaktInfo = melding.getKontaktInfo();
        KontaktpersonData kontaktpersonData = aktivitetDTO.getStillingFraNavData().getKontaktpersonData();
        if (meldingKontaktInfo == null) {
            assertEquals(null, kontaktpersonData);
        } else {
            Assertions.assertThat(meldingKontaktInfo).isEqualToIgnoringNullFields(kontaktpersonData);
        }
        return aktivitetDTO;
    }

    public static ForesporselOmDelingAvCv createMelding(String bestillingsId, MockBruker mockBruker) {
        return ForesporselOmDelingAvCv.newBuilder()
                .setAktorId(mockBruker.getAktorId())
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

    /**
     * Henter alle aktiviteter for et fnr via aktivitet-apiet.
     *
     * @param port       Portnummeret til webserveren.
     *                   Når man bruker SpringBootTest.WebEnvironment.RANDOM_PORT, kan portnummeret injektes i testklassen ved å bruke @code{\@LocalServerPort private int port;}
     * @param mockBruker mock bruker
     * @return En AktivitetplanDTO med en liste av AktivitetDto
     */
    public AktivitetsplanDTO hentAktiviteterForFnr(int port, MockBruker mockBruker) {
        return hentAktiviteterForFnr(port, mockBruker, mockBruker);
    }


    public AktivitetsplanDTO hentAktiviteterForFnr(int port, MockBruker mockBruker, RestassuredUser user) {
        Response response = user
                .createRequest()
                .get(user.getUrl("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet", mockBruker))
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .response();

        return response.as(AktivitetsplanDTO.class);
    }

    public List<AktivitetDTO> hentVersjoner(String aktivitetId, int port, MockBruker mockBruker, RestassuredUser user) {
        Response response = user
                .createRequest()
                .get(user.getUrl("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet/" + aktivitetId + "/versjoner", mockBruker))
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .response();
        return response.jsonPath().getList(".", AktivitetDTO.class);
    }


    /**
     * Oppretter en ny aktivitet via aktivitet-apiet. Kallet blir utført av nav-bruker no.nav.veilarbaktivitet.config.FilterTestConfig#NAV_IDENT_ITEST Z123456
     *
     * @param port         Portnummeret til webserveren.
     *                     Når man bruker SpringBootTest.WebEnvironment.RANDOM_PORT, kan portnummeret injektes i testklassen ved å bruke @code{\@LocalServerPort private int port;}
     * @param mockBruker   Brukeren man skal opprette aktiviteten for
     * @param aktivitetDTO payload
     * @return Aktiviteten
     */
    public AktivitetDTO opprettAktivitet(int port, MockBruker mockBruker, AktivitetDTO aktivitetDTO) {

        return opprettAktivitet(port, mockBruker, mockBruker, aktivitetDTO);
    }

    public AktivitetDTO opprettAktivitet(int port, MockBruker mockBruker, RestassuredUser user, AktivitetDTO aktivitetDTO) {

        String aktivitetPayloadJson = JsonUtils.toJson(aktivitetDTO);

        Response response = user
                .createRequest()
                .and()
                .body(aktivitetPayloadJson)
                .when()
                .post(user.getUrl("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet/ny", mockBruker))
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();

        AktivitetDTO aktivitet = response.as(AktivitetDTO.class);
        assertNotNull(aktivitet);
        assertNotNull(aktivitet.getId());
        AktivitetAssertUtils.assertOpprettetAktivitet(aktivitet, aktivitetDTO);

        return aktivitet;
    }

    public static AktivitetDTO finnAktivitet(AktivitetsplanDTO aktivitetsplanDTO, String id) {
        return aktivitetsplanDTO.aktiviteter.stream().filter(a -> a.getId().equals(id)).findAny().get();
    }
}
