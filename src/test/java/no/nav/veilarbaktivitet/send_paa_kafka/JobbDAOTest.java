package no.nav.veilarbaktivitet.send_paa_kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.domain.*;
import no.nav.veilarbaktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.util.MockBruker;
import no.nav.veilarbaktivitet.util.TestService;
import no.nav.veilarbaktivitet.util.WireMockUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import io.restassured.response.Response;

import java.util.Date;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.*;
import static org.springframework.boot.test.context.SpringBootTest.*;

@Slf4j
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@EmbeddedKafka(topics = {"${topic.inn.stillingFraNav}","${topic.ut.stillingFraNav}"}, partitions = 1)
@AutoConfigureWireMock(port = 0)
@Transactional
public class JobbDAOTest {

    @Autowired
    private JobbDAO jobbDAO;

    @Autowired
    private TestService testService;

    @LocalServerPort
    int port;

    @Test
    public void opprettJobbTest() {
        MockBruker mockBruker = MockBruker.happyBruker("kake", "pizza");

        AktivitetDTO aktivitetDTO = testService.opprettAktivitet(port, mockBruker, AktivitetDTOMapper.mapTilAktivitetDTO(AktivitetDataTestBuilder.nyStillingFraNav(), false));

        JobbDTO jobbDTO = JobbDTO.builder()
                .jobbType(JobbType.REKRUTTERINGSBISTAND_KAFKA)
                .aktivitetId(Long.parseLong(aktivitetDTO.getId()))
                .versjon(Long.parseLong(aktivitetDTO.getVersjon()))
                .status(Status.PENDING)
                .build();

        jobbDAO.insertJobb(jobbDTO);
        List<JobbDTO> aktivitetJobber = jobbDAO.hentJobber();

        assertEquals(1, aktivitetJobber.size());

        JobbDTO actual = aktivitetJobber.stream().findFirst().get();
        assertNotNull(actual.getId());

        JobbDTO expected = jobbDTO.withId(actual.getId());
        assertEquals(expected, actual);
    }


}