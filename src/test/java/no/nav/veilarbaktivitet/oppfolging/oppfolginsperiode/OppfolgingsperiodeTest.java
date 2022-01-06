package no.nav.veilarbaktivitet.oppfolging.oppfolginsperiode;

import no.nav.veilarbaktivitet.SpringBootBaseTest;
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

public class OppfolgingsperiodeTest extends SpringBootBaseTest {

    @Autowired
    OppfolgingsperiodeCron oppfolgingsperiodeAdderCron;

    @Test
    public void skalLeggeTilOppfolgingsperioder() {
        MockBruker mockBruker = MockNavService.createHappyBruker();

        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO opprettet = testAktivitetservice.opprettAktivitet(port, mockBruker, aktivitetDTO);

        assertNull(opprettet.getOppfolgingsperiodeId());

        oppfolgingsperiodeAdderCron.addOppfolgingsperioder();

        AktivitetDTO etterAdd = testAktivitetservice.hentAktivitet(port, mockBruker, opprettet.getId());

        assertEquals(mockBruker.getOppfolgingsperiode(), etterAdd.getOppfolgingsperiodeId());
    }
}
