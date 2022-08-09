package no.nav.veilarbaktivitet.stilling_fra_nav;

import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaJsonTemplate;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.person.InnsenderData;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class CvDeltITest extends SpringBootTestBase {

    @Autowired
    KafkaJsonTemplate<RekrutteringsbistandStatusoppdatering> navCommonJsonProducerFactory;

    @Value("${topic.inn.rekrutteringsbistandStatusoppdatering}")
    private String innRekrutteringsbistandStatusoppdatering;

    @Test
    public void behandleCvDeltHappyCase() throws Exception {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);
        String navIdent = "E271828";

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);
        Date tidspunkt1 = Date.from(Instant.ofEpochSecond(1));
        aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, aktivitetDTO, tidspunkt1);

        AktivitetDTO aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());

        String bestillingsId = aktivitetDTO.getStillingFraNavData().bestillingsId;

        Date tidspunkt2 = Date.from(Instant.now());
        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering =
                new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, "", navIdent, tidspunkt2);

        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult = navCommonJsonProducerFactory.send(innRekrutteringsbistandStatusoppdatering, bestillingsId, sendtStatusoppdatering).get(5, TimeUnit.SECONDS);

        kafkaTestService.assertErKonsumertAiven(innRekrutteringsbistandStatusoppdatering, sendResult.getRecordMetadata().offset(), 5);


        AktivitetDTO aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(aktivitetData_etter.getVersjon()).isGreaterThan(aktivitetData_for.getVersjon());
            assertions.assertThat(aktivitetData_etter.getEndretAv()).isEqualTo(navIdent);
            assertions.assertThat(aktivitetData_etter.getLagtInnAv()).isEqualTo(InnsenderData.NAV.name());
            assertions.assertThat(aktivitetData_etter.getStatus()).isSameAs(aktivitetData_for.getStatus());
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData()).isNotNull();
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getSoknadsstatus()).isSameAs(Soknadsstatus.CV_DELT);
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getLivslopsStatus()).isSameAs(aktivitetData_for.getStillingFraNavData().getLivslopsStatus());
            assertions.assertAll();
        });
    }
}
