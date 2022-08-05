package no.nav.veilarbaktivitet.stilling_fra_nav;

import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaJsonTemplate;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.Date;

public class CvDeltServiceTest extends SpringBootTestBase {

    @Autowired
    KafkaJsonTemplate<RekrutteringsbistandStatusoppdatering> navCommonJsonProducerFactory;

    @Value("${topic.inn.rekrutteringsbistandStatusoppdatering}")
    private String rekrutteringsbistandstatusoppdateringtopic;

    @Autowired
    DelingAvCvDAO delingAvCvDAO;

    @Autowired
    CvDeltService cvDeltService;

    @Test
    public void happy_case_svar_ja() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);
        Date tidspunkt = Date.from(Instant.ofEpochSecond(1));
        aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, aktivitetDTO, tidspunkt);

        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering =
                new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, "", "P314159", tidspunkt);

        String key = aktivitetDTO.getStillingFraNavData().bestillingsId;
        navCommonJsonProducerFactory.send(rekrutteringsbistandstatusoppdateringtopic, key, sendtStatusoppdatering);

        Assertions.assertThat(Boolean.TRUE).as("Hvis vi kommer hit er alt bra").isTrue();
    }
}