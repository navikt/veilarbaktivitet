package no.nav.veilarbaktivitet.util;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetsplanDTO;
import no.nav.veilarbaktivitet.arena.model.AktiviteterDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.avro.DelingAvCvRespons;
import no.nav.veilarbaktivitet.avtalt_med_nav.Forhaandsorientering;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDTO;
import no.nav.veilarbaktivitet.avtalt_med_nav.Type;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.mock_nav_modell.RestassuredUser;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.stilling_fra_nav.KontaktpersonData;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavTestService;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.KontaktInfo;
import no.nav.veilarbaktivitet.testutils.AktivitetAssertUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.springframework.http.HttpStatus;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static no.nav.veilarbaktivitet.arena.VeilarbarenaMapper.prefixArenaId;
import static no.nav.veilarbaktivitet.config.ApplicationContext.ARENA_AKTIVITET_DATOFILTER_PROPERTY;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;


@RequiredArgsConstructor
public class AktivitetTestService {
    private final StillingFraNavTestService stillingFraNavTestService;
    private final int port;

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

    public AktivitetDTO oppdatterAktivitet(int port, MockBruker mockBruker, RestassuredUser user, AktivitetDTO aktivitetDTO) {
        String aktivitetPayloadJson = JsonUtils.toJson(aktivitetDTO);

        Response response = user
                .createRequest()
                .and()
                .body(aktivitetPayloadJson)
                .when()
                .put(user.getUrl("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet/" + aktivitetDTO.getId(), mockBruker))
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();

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
        return aktivitetsplanDTO.aktiviteter.stream().filter(a -> a.getId().equals(id)).findAny().get();
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

    public AktivitetDTO opprettStillingFraNav(MockBruker mockBruker, ForesporselOmDelingAvCv melding) {
        ConsumerRecord<String, DelingAvCvRespons> stillingFraNavRecord = stillingFraNavTestService.opprettStillingFraNav(mockBruker, melding, port);
        DelingAvCvRespons value = stillingFraNavRecord.value();
        AktivitetsplanDTO aktivitetsplanDTO = this.hentAktiviteterForFnr(mockBruker);
        AktivitetDTO aktivitetDTO = aktivitetsplanDTO.getAktiviteter().stream().filter(a -> a.getId().equals(value.getAktivitetId())).findAny().get();

        //TODO skriv bedre test
        assertEquals(AktivitetTypeDTO.STILLING_FRA_NAV, aktivitetDTO.getType());
        assertEquals(melding.getStillingstittel(), aktivitetDTO.getTittel());
        assertEquals(null, aktivitetDTO.getLenke());
        assertEquals(melding.getBestillingsId(), aktivitetDTO.getStillingFraNavData().getBestillingsId());

        KontaktInfo meldingKontaktInfo = melding.getKontaktInfo();
        KontaktpersonData kontaktpersonData = aktivitetDTO.getStillingFraNavData().getKontaktpersonData();
        if (meldingKontaktInfo == null) {
            assertEquals(null, kontaktpersonData);
        } else if (kontaktpersonData == null) {
            assertTrue(meldingKontaktInfo.getMobil() == null || meldingKontaktInfo.getMobil().equals(""));
            assertTrue(meldingKontaktInfo.getTittel() == null || meldingKontaktInfo.getTittel().equals(""));
            assertTrue(meldingKontaktInfo.getNavn() == null || meldingKontaktInfo.getNavn().equals(""));
        } else {
            Assertions.assertThat(meldingKontaktInfo).isEqualToComparingFieldByField(kontaktpersonData);
        }
        return aktivitetDTO;
    }

    public static ForesporselOmDelingAvCv createForesporselOmDelingAvCv(String bestillingsId, MockBruker mockBruker) {
        return StillingFraNavTestService.createForesporselOmDelingAvCv(bestillingsId, mockBruker);
    }


    public AktivitetDTO svarPaaDelingAvCv(boolean kanDeleCv, MockBruker mockBruker, MockVeileder veileder, AktivitetDTO aktivitetDTO, Date avtaltDato) {
        return StillingFraNavTestService.svarPaaDelingAvCv(kanDeleCv, mockBruker, veileder, aktivitetDTO, avtaltDato, port);

    }

    public AktivitetDTO oppdatterAktivitetStatus(MockBruker mockBruker, MockVeileder user, AktivitetDTO orginalAktivitet, AktivitetStatus status) {
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

    public ArenaAktivitetDTO opprettFHO(MockBruker mockBruker, String arenaaktivitetId) {
        stub_hent_arenaaktiviteter(mockBruker.getFnrAsFnr(), arenaaktivitetId);

        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);

        System.setProperty(ARENA_AKTIVITET_DATOFILTER_PROPERTY, "2018-01-01");

        ForhaandsorienteringDTO forhaandsorienteringDTO = ForhaandsorienteringDTO.builder()
                .id("id")
                .type(Type.SEND_FORHAANDSORIENTERING)
                .tekst("asd")
                .lestDato(null)
                .build();

        Response response = mockVeileder
                .createRequest()
                .and()
                .param("arenaaktivitetId", arenaaktivitetId)
                .body(forhaandsorienteringDTO)
                .when()
                .put(mockVeileder.getUrl("http://localhost:" + port + "/veilarbaktivitet/api/arena/forhaandsorientering", mockBruker))
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();

        return response.as(ArenaAktivitetDTO.class);
    }

    private void stub_hent_arenaaktiviteter(Person.Fnr fnr, String arenaaktivitetId) {
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
                                """.formatted(arenaaktivitetId))));
    }
}
