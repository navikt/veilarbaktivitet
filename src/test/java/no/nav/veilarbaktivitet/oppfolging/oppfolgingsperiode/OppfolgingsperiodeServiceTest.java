package no.nav.veilarbaktivitet.oppfolging.oppfolgingsperiode;

import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OppfolgingsperiodeServiceTest extends SpringBootTestBase {

    @Autowired
    OppfolgingsperiodeCron oppfolgingsperiodeCron;

    @Test
    public void skalLeggeTilOppfolgingsperioder() {
        MockBruker mockBruker = MockNavService.createHappyBruker();

        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO opprettet = testAktivitetservice.opprettAktivitet(port, mockBruker, aktivitetDTO);

        assertNull(opprettet.getOppfolgingsperiodeId());

        oppfolgingsperiodeCron.addOppfolgingsperioder();

        AktivitetDTO etterAdd = testAktivitetservice.hentAktivitet(port, mockBruker, opprettet.getId());

        assertEquals(mockBruker.getOppfolgingsperiode(), etterAdd.getOppfolgingsperiodeId());
    }

    @Test(timeout = 5000)
    public void skalHåntereGamleAktiviteterUtenPeriode() {
        MockBruker mockBruker = MockNavService.createHappyBruker();

        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO opprettet = testAktivitetservice.opprettAktivitet(port, mockBruker, aktivitetDTO);
        jdbc.update("update AKTIVITET set OPPRETTET_DATO = DATE '2017-08-01' where AKTIVITET_ID = " + opprettet.getId());

        assertNull(opprettet.getOppfolgingsperiodeId());

        oppfolgingsperiodeCron.addOppfolgingsperioder();

        AktivitetDTO etterAdd = testAktivitetservice.hentAktivitet(port, mockBruker, opprettet.getId());

        assertNull(etterAdd.getOppfolgingsperiodeId());
    }
}
