package no.nav.veilarbaktivitet.oppfolging.oppfolgingsperiode;

import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OppfolgingsperiodeServiceTest extends SpringBootTestBase {

    @Autowired
    OppfolgingsperiodeCron oppfolgingsperiodeCron;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Test
    public void skalLeggeTilOppfolgingsperioder() {
        MockBruker mockBruker = MockNavService.createHappyBruker();

        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO opprettet = testAktivitetservice.opprettAktivitet(port, mockBruker, aktivitetDTO);

        assertNull(opprettet.getOppfolgingsperiodeId());

        transactionTemplate.executeWithoutResult( ts ->  {
            oppfolgingsperiodeCron.addOppfolgingsperioder();
            ts.flush();
        });
        //oppfolgingsperiodeCron.addOppfolgingsperioder();

        AktivitetDTO etterAdd = testAktivitetservice.hentAktivitet(port, mockBruker, opprettet.getId());

        assertEquals(mockBruker.getOppfolgingsperiode(), etterAdd.getOppfolgingsperiodeId());
    }
}
