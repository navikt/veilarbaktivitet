package no.nav.veilarbaktivitet.util;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetsplanDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.EtikettTypeDTO;
import no.nav.veilarbaktivitet.aktivitetskort.ArenaKort;
import no.nav.veilarbaktivitet.aktivitetskort.ArenaMeldingHeaders;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.KasseringsBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.dto.KafkaAktivitetskortWrapperDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.avro.DelingAvCvRespons;
import no.nav.veilarbaktivitet.avtalt_med_nav.AvtaltMedNavDTO;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDTO;
import no.nav.veilarbaktivitet.avtalt_med_nav.LestDTO;
import no.nav.veilarbaktivitet.avtalt_med_nav.Type;
import no.nav.veilarbaktivitet.config.kafka.NavCommonKafkaConfig;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaJsonTemplate;
import no.nav.veilarbaktivitet.internapi.model.Aktivitet;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.mock_nav_modell.RestassuredUser;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.stilling_fra_nav.KontaktpersonData;
import no.nav.veilarbaktivitet.stilling_fra_nav.RekrutteringsbistandStatusoppdatering;
import no.nav.veilarbaktivitet.stilling_fra_nav.RekrutteringsbistandStatusoppdateringEventType;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavTestService;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.KontaktInfo;
import no.nav.veilarbaktivitet.testutils.AktivitetAssertUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.assertj.core.api.Assertions;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static no.nav.veilarbaktivitet.aktivitetskort.AktivitetsbestillingCreator.*;
import static no.nav.veilarbaktivitet.config.ApplicationContext.ARENA_AKTIVITET_DATOFILTER_PROPERTY;
import static org.junit.jupiter.api.Assertions.*;


@RequiredArgsConstructor
public class AktivitetTestService {
    private final StillingFraNavTestService stillingFraNavTestService;
    private final int port;

    private final String innRekrutteringsbistandStatusoppdateringTopic;

    private final KafkaTestService kafkaTestService;

    private final KafkaTemplate<String, String> stringStringKafkaTemplate;

    private final KafkaJsonTemplate<RekrutteringsbistandStatusoppdatering> rekrutteringsbistandStatusoppdateringProducer;

    private final String aktivitetsKortV1Topic;

    /**
     * Henter alle aktiviteter for et fnr via aktivitet-apiet.
     *
     * @param mockBruker mock bruker
     * @return En AktivitetplanDTO med en liste av AktivitetDto
     */
    public AktivitetsplanDTO hentAktiviteterForFnr(MockBruker mockBruker) {
        return hentAktiviteterForFnr(mockBruker, mockBruker);
    }


    public AktivitetsplanDTO hentAktiviteterForFnr(MockBruker mockBruker, RestassuredUser user) {
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

    public List<AktivitetDTO> hentVersjoner(String aktivitetId, MockBruker mockBruker, RestassuredUser user) {
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
     * @param mockBruker   Brukeren man skal opprette aktiviteten for
     * @param aktivitetDTO payload
     * @return Aktiviteten
     */
    public AktivitetDTO opprettAktivitet(MockBruker mockBruker, AktivitetDTO aktivitetDTO) {
        return opprettAktivitet(mockBruker, mockBruker, aktivitetDTO);
    }

    public AktivitetDTO opprettAktivitet(MockBruker mockBruker, RestassuredUser user, AktivitetDTO aktivitetDTO) {
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

    public ValidatableResponse oppdatterAktivitet(MockBruker mockBruker, RestassuredUser user, AktivitetDTO aktivitetDTO) {
        String aktivitetPayloadJson = JsonUtils.toJson(aktivitetDTO);

        return user
                .createRequest()
                .and()
                .body(aktivitetPayloadJson)
                .when()
                .put(user.getUrl("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet/" + aktivitetDTO.getId(), mockBruker))
                .then();
    }
    public AktivitetDTO oppdaterAktivitetOk(MockBruker mockBruker, RestassuredUser user, AktivitetDTO aktivitetDTO) {
        Response response = oppdatterAktivitet(mockBruker, user, aktivitetDTO)
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response();

        AktivitetDTO aktivitet = response.as(AktivitetDTO.class);
        assertNotNull(aktivitet);
        assertNotNull(aktivitet.getId());
        AktivitetAssertUtils.assertOppdatertAktivitet(aktivitet, aktivitetDTO);

        return aktivitet;
    }


    public AktivitetDTO opprettAktivitetSomVeileder(MockVeileder veileder, MockBruker mockBruker, AktivitetDTO aktivitetDTO) {
        return opprettAktivitet(mockBruker, veileder, aktivitetDTO);
    }

    public static AktivitetDTO finnAktivitet(AktivitetsplanDTO aktivitetsplanDTO, String id) {
        return aktivitetsplanDTO.getAktiviteter().stream().filter(a -> a.getId().equals(id)).findAny().get();
    }

    public AktivitetDTO hentAktivitet(MockBruker mockBruker, String id) {
        return hentAktivitet(mockBruker, mockBruker, id);
    }

    public AktivitetDTO hentAktivitet(MockBruker mockBruker, RestassuredUser user, String id) {
        Response response = user
                .createRequest()
                .get(user.getUrl("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet/" + id, mockBruker))
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .response();

        return response.as(AktivitetDTO.class);
    }

    public AktivitetDTO opprettStillingFraNav(MockBruker mockBruker) {
        return opprettStillingFraNav(mockBruker, createForesporselOmDelingAvCv(UUID.randomUUID().toString(), mockBruker));
    }

    public void mottaOppdateringFraRekrutteringsbistand(String bestillingsId, String detaljer, RekrutteringsbistandStatusoppdateringEventType oppdateringsType, String endretAvIdent, ZonedDateTime tidspunkt) throws ExecutionException, InterruptedException, TimeoutException {
        var payload = new RekrutteringsbistandStatusoppdatering(
                oppdateringsType,
                detaljer,
                endretAvIdent,
                tidspunkt
        );
        var sendResultFattJobben = rekrutteringsbistandStatusoppdateringProducer.send(
                innRekrutteringsbistandStatusoppdateringTopic,
                bestillingsId,
                payload
        ).get(5, TimeUnit.SECONDS);
        kafkaTestService.assertErKonsumert(
                innRekrutteringsbistandStatusoppdateringTopic,
                sendResultFattJobben.getRecordMetadata().offset()
        );
    }

    public AktivitetDTO opprettStillingFraNav(MockBruker mockBruker, ForesporselOmDelingAvCv melding) {
        ConsumerRecord<String, DelingAvCvRespons> stillingFraNavRecord = stillingFraNavTestService.opprettStillingFraNav(mockBruker, melding);
        DelingAvCvRespons value = stillingFraNavRecord.value();
        AktivitetsplanDTO aktivitetsplanDTO = this.hentAktiviteterForFnr(mockBruker);
        AktivitetDTO aktivitetDTO = aktivitetsplanDTO.getAktiviteter().stream().filter(a -> a.getId().equals(value.getAktivitetId())).findAny().get();

        //TODO skriv bedre test
        assertEquals(AktivitetTypeDTO.STILLING_FRA_NAV, aktivitetDTO.getType());
        assertEquals(melding.getStillingstittel(), aktivitetDTO.getTittel());
        assertNull(aktivitetDTO.getLenke());
        assertEquals(melding.getBestillingsId(), aktivitetDTO.getStillingFraNavData().getBestillingsId());

        KontaktInfo meldingKontaktInfo = melding.getKontaktInfo();
        KontaktpersonData kontaktpersonData = aktivitetDTO.getStillingFraNavData().getKontaktpersonData();
        if (meldingKontaktInfo == null) {
            assertNull(kontaktpersonData);
        } else if (kontaktpersonData == null) {
            assertTrue(meldingKontaktInfo.getMobil() == null || meldingKontaktInfo.getMobil().equals(""));
            assertTrue(meldingKontaktInfo.getTittel() == null || meldingKontaktInfo.getTittel().equals(""));
            assertTrue(meldingKontaktInfo.getNavn() == null || meldingKontaktInfo.getNavn().equals(""));
        } else {
            Assertions.assertThat(meldingKontaktInfo).usingRecursiveComparison().isEqualTo(kontaktpersonData);
        }
        return aktivitetDTO;
    }

    public static ForesporselOmDelingAvCv createForesporselOmDelingAvCv(String bestillingsId, MockBruker mockBruker) {
        return StillingFraNavTestService.createForesporselOmDelingAvCv(bestillingsId, mockBruker);
    }


    public AktivitetDTO svarPaaDelingAvCv(boolean kanDeleCv, MockBruker mockBruker, MockVeileder veileder, AktivitetDTO aktivitetDTO, Date avtaltDato) {
        return StillingFraNavTestService.svarPaaDelingAvCv(kanDeleCv, mockBruker, veileder, aktivitetDTO, avtaltDato, port);

    }

    public AktivitetDTO oppdaterAktivitetStatus(MockBruker mockBruker, MockVeileder user, AktivitetDTO orginalAktivitet, AktivitetStatus status) {
        AktivitetDTO aktivitetDTO = orginalAktivitet.toBuilder().status(status).build();
        String aktivitetPayloadJson = JsonUtils.toJson(aktivitetDTO);

        Response response = user
                .createRequest()
                .and()
                .body(aktivitetPayloadJson)
                .when()
                .put(user.getUrl("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet/" + aktivitetDTO.getId() + "/status", mockBruker))
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();

        AktivitetDTO aktivitet = response.as(AktivitetDTO.class);
        assertNotNull(aktivitet);
        assertNotNull(aktivitet.getId());
        AktivitetAssertUtils.assertOppdatertAktivitet(aktivitet, aktivitetDTO);

        return aktivitet;
    }

    public AktivitetDTO oppdaterAktivitetEtikett(MockBruker mockBruker, MockVeileder user, AktivitetDTO orginalAktivitet, EtikettTypeDTO status) {
        AktivitetDTO aktivitetDTO = orginalAktivitet.toBuilder().etikett(status).build();
        String aktivitetPayloadJson = JsonUtils.toJson(aktivitetDTO);

        Response response = user
                .createRequest()
                .and()
                .body(aktivitetPayloadJson)
                .when()
                .put(user.getUrl("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet/" + aktivitetDTO.getId() + "/etikett", mockBruker))
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();

        AktivitetDTO aktivitet = response.as(AktivitetDTO.class);
        assertNotNull(aktivitet);
        assertNotNull(aktivitet.getId());
        AktivitetAssertUtils.assertOppdatertAktivitet(aktivitet, aktivitetDTO);

        return aktivitet;
    }

    public ArenaAktivitetDTO opprettFHOForArenaAktivitet(MockBruker mockBruker, ArenaId arenaaktivitetId, MockVeileder veileder) {
        stub_hent_arenaaktiviteter_fra_veilarbarena(mockBruker.getFnrAsFnr(), arenaaktivitetId.id());
        System.setProperty(ARENA_AKTIVITET_DATOFILTER_PROPERTY, "2018-01-01");

        ForhaandsorienteringDTO forhaandsorienteringDTO = ForhaandsorienteringDTO.builder()
                .id("id")
                .type(Type.SEND_FORHAANDSORIENTERING)
                .tekst("asd")
                .lestDato(null)
                .build();

        Response response = veileder
                .createRequest()
                .and()
                .param("arenaaktivitetId", arenaaktivitetId.id())
                .body(forhaandsorienteringDTO)
                .when()
                .put(veileder.getUrl("/veilarbaktivitet/api/arena/forhaandsorientering", mockBruker))
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();

        return response.as(ArenaAktivitetDTO.class);
    }


    public ValidatableResponse opprettFHOForInternAktivitetRequest(MockBruker mockBruker, MockVeileder veileder, AvtaltMedNavDTO avtaltDTO, long aktivitetId) {
        return veileder
                .createRequest(mockBruker)
                .and()
                .body(avtaltDTO)
                .when()
                .queryParam("aktivitetId", aktivitetId)
                .put("http://localhost:" + port + "/veilarbaktivitet/api/avtaltMedNav")
                .then();
    }
    public AktivitetDTO opprettFHOForInternAktivitet(MockBruker mockBruker, MockVeileder veileder, AvtaltMedNavDTO avtaltDTO, long aktivitetId) {
        return opprettFHOForInternAktivitetRequest(mockBruker, veileder, avtaltDTO, aktivitetId)
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .response()
                .as(AktivitetDTO.class);
    }

    private void stub_hent_arenaaktiviteter_fra_veilarbarena(Person.Fnr fnr, String arenaaktivitetId) {
        stubFor(get("/veilarbarena/api/arena/aktiviteter?fnr=" + fnr.get())
                .willReturn(aResponse().withStatus(200)
                        .withBody("""
                                {
                                  "tiltaksaktiviteter": [
                                      {
                                        "tiltaksnavn": "tiltaksnavn",
                                        "aktivitetId": "%s",
                                        "tiltakLokaltNavn": "lokaltnavn",
                                        "arrangor": "arrangor",
                                        "bedriftsnummer": "asd",
                                        "deltakelsePeriode": {
                                            "fom": "2021-11-18",
                                            "tom": "2021-11-25"
                                        },
                                        "deltakelseProsent": 60,
                                        "deltakerStatus": "GJENN",
                                        "statusSistEndret": "2021-11-18",
                                        "begrunnelseInnsoking": "asd",
                                        "antallDagerPerUke": 3.0
                                      }
                                  ],
                                  "gruppeaktiviteter": [],
                                  "utdanningsaktiviteter": []
                                }
                                """.formatted(removeArenaPrefix(arenaaktivitetId)))));
    }

    private String removeArenaPrefix(String arenaId) {
        return arenaId.replace("ARENA", "");
    }
    public AktivitetDTO lesFHO(MockBruker mockBruker, long aktivitetsId, long versjon) {
        Response response = mockBruker
                .createRequest()
                .and()
                .body(JsonUtils.toJson(new LestDTO(
                    aktivitetsId,
                    versjon
                )))
                .when()
                .put(mockBruker.getUrl("/veilarbaktivitet/api/avtaltMedNav/lest", mockBruker))
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();

        return response.as(AktivitetDTO.class);
    }

    public List<ArenaAktivitetDTO> hentArenaAktiviteter(MockBruker bruker, ArenaId arenaId) {
        stub_hent_arenaaktiviteter_fra_veilarbarena(bruker.getFnrAsFnr(), arenaId.id());
        return bruker
                .createRequest()
                .get(bruker.getUrl("/veilarbaktivitet/api/arena/tiltak", bruker))
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .response()
                .jsonPath().getList(".", ArenaAktivitetDTO.class);
    }

    public List<Aktivitet> hentAktiviteterInternApi(MockVeileder veileder, Person.AktorId aktorId) {
        return veileder
                .createRequest()
                .get("/veilarbaktivitet/internal/api/v1/aktivitet?aktorId={aktorId}", aktorId.get())
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .response()
                .jsonPath().getList(".", Aktivitet.class);
    }


    public ProducerRecord makeAktivitetskortProducerRecord(KafkaAktivitetskortWrapperDTO melding, ArenaMeldingHeaders arenaMeldingHeaders) {
        if (arenaMeldingHeaders == null) {
            return new ProducerRecord<>(aktivitetsKortV1Topic, melding.getAktivitetskortId().toString(), JsonUtils.toJson(melding));
        }

        ArrayList<Header> headers = new ArrayList<>(List.of(
                new RecordHeader(HEADER_EKSTERN_REFERANSE_ID, arenaMeldingHeaders.eksternReferanseId().id().getBytes()),
                new RecordHeader(HEADER_EKSTERN_ARENA_TILTAKSKODE, arenaMeldingHeaders.arenaTiltakskode().getBytes())
        ));
        if (arenaMeldingHeaders.oppfolgingsperiodeSlutt() != null) {
            headers.add(new RecordHeader(HEADER_OPPFOLGINGSPERIODE_SLUTT, arenaMeldingHeaders.oppfolgingsperiodeSlutt().toString().getBytes()));
        }
        if (arenaMeldingHeaders.oppfolgingsperiode() != null) {
            headers.add(new RecordHeader(HEADER_OPPFOLGINGSPERIODE, arenaMeldingHeaders.oppfolgingsperiode().toString().getBytes()));
        }
        return new ProducerRecord<>(aktivitetsKortV1Topic, null, melding.getAktivitetskortId().toString(), JsonUtils.toJson(melding), headers);
    }

    public void opprettEksterntArenaKort(ArenaKort arenaKort) {
        opprettEksterntArenaKort(List.of(arenaKort));
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public void opprettEksterntArenaKort(List<ArenaKort> arenaKort) {
        var lastRecord = (SendResult<String, String>) arenaKort.stream().map(
                    kort -> makeAktivitetskortProducerRecord(kort.getMelding(), kort.getHeader()))
                .map((record) -> stringStringKafkaTemplate.send(record))
                .skip(arenaKort.size() - 1)
                .findFirst().get().get();

        kafkaTestService.assertErKonsumert(aktivitetsKortV1Topic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, lastRecord.getRecordMetadata().offset());
    }

    @SneakyThrows
    public SendResult opprettEksterntAktivitetsKort(ProducerRecord<String, String> producerRecord) {
        var sendResult = stringStringKafkaTemplate.send(producerRecord).get();
        kafkaTestService.assertErKonsumert(aktivitetsKortV1Topic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, sendResult.getRecordMetadata().offset());
        return sendResult;
    }

    @SneakyThrows
    public void kasserEskterntAktivitetskort(KasseringsBestilling kasseringsBestilling) {
        var record = new ProducerRecord<>(aktivitetsKortV1Topic, kasseringsBestilling.getAktivitetskortId().toString(), JsonUtils.toJson(kasseringsBestilling));
        var recordMetadata = stringStringKafkaTemplate.send(record).get();
        kafkaTestService.assertErKonsumert(aktivitetsKortV1Topic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, recordMetadata.getRecordMetadata().offset());
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public void opprettEksterntAktivitetsKort(List<KafkaAktivitetskortWrapperDTO> meldinger) {
        var lastRecord = (SendResult<String, String>) meldinger.stream()
                .map((melding) -> makeAktivitetskortProducerRecord(melding, null))
                .map((record) -> stringStringKafkaTemplate.send(record))
                .skip(meldinger.size() - 1)
                .findFirst().get().get();

        kafkaTestService.assertErKonsumert(aktivitetsKortV1Topic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, lastRecord.getRecordMetadata().offset());
    }

    public AktivitetDTO hentAktivitetByFunksjonellId(MockBruker mockBruker, MockVeileder veileder, UUID funksjonellId) {
        return hentAktiviteterForFnr(mockBruker, veileder)
                .getAktiviteter().stream()
                .filter((a) -> Objects.equals(a.getFunksjonellId(), funksjonellId))
                .findFirst().orElseThrow(() -> new IllegalStateException(String.format("Fant ikke aktivitet med funksjonellId %s", funksjonellId)));
    }
}
