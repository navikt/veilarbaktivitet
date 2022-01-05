package no.nav.veilarbaktivitet.oppfolging.oppfolginsperiode_adder;

import no.nav.veilarbaktivitet.SpringTestClass;
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

public class OppfolingsperiodeAdderTest extends SpringTestClass {
    @Autowired
    OppfolingsperiodeAdder addder;

    @Test
    public void skalLeggeTillOppfolingsperioder() {
        MockBruker mockBruker = MockNavService.crateHappyBruker();

        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO oprrettet = testAktivitetservice.opprettAktivitet(port, mockBruker, aktivitetDTO);

        assertNull(oprrettet.getOppfolingsPeriodeId());

        addder.addOppfolingsperioderForEnBruker();

        AktivitetDTO etterAdd = testAktivitetservice.hentAktivitet(port, mockBruker, oprrettet.getId());

        assertEquals(mockBruker.getOppfolgingsPeriode(), etterAdd.getOppfolingsPeriodeId());
    }
}
