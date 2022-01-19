package no.nav.veilarbaktivitet.aktivitet;


import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.testutils.AktivitetDTOTestBuilder;
import org.junit.Test;

import static no.nav.veilarbaktivitet.testutils.AktivitetAssertUtils.assertOppdatertAktivitet;

public class AktivitetServiceTest extends SpringBootTestBase {

    @Test
    public void skalKunneOppretteAktiviteter() {
        MockBruker happyBruker = MockNavService.createHappyBruker();

        for(AktivitetTypeDTO type:  AktivitetTypeDTO.values()) {
            if(type == AktivitetTypeDTO.STILLING_FRA_NAV) {
                continue;
            }

            AktivitetDTO aktivitetDTO = AktivitetDTOTestBuilder.nyAktivitet(type);
            AktivitetDTO opprettAktivitet = testAktivitetservice.opprettAktivitet(port, happyBruker, aktivitetDTO);
            assertOppdatertAktivitet(aktivitetDTO, opprettAktivitet);
        }
    }

}