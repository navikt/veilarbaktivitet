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

public class OppfolgingsperiodeAdderTest extends SpringTestClass {
    @Autowired
    OppfolgingsperiodeAdder adder;

    @Test
    public void skalLeggeTilOppfolgingsperioder() {
        MockBruker mockBruker = MockNavService.crateHappyBruker();

        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO opprettet = testAktivitetservice.opprettAktivitet(port, mockBruker, aktivitetDTO);

        assertNull(opprettet.getOppfolgingsperiodeId());

        adder.addOppfolingsperioderForEnBruker();

        AktivitetDTO etterAdd = testAktivitetservice.hentAktivitet(port, mockBruker, opprettet.getId());

        assertEquals(mockBruker.getOppfolgingsPeriode(), etterAdd.getOppfolgingsperiodeId());
    }
}
