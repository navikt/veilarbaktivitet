package no.nav.veilarbaktivitet.oppfolging.oppfolgingsperiode;

import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

public class SamskjorAktivitetTest extends SpringBootTestBase {
    @Autowired
    OppfolgingsperiodeCron oppfolgingsperiodeCron;

    @Test
    public void samksjorAktiviteter() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        AktivitetDTO aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN);
        aktivitetDTO.setTittel("OrginalTittel");
        AktivitetDTO aktivitet = aktivitetTestService.opprettAktivitet(port, happyBruker, aktivitetDTO);
        aktivitet.setTittel("nyTittel");
        AktivitetDTO oppdatertAktivitet = aktivitetTestService.oppdatterAktivitet(port, happyBruker, happyBruker, aktivitet);

        jdbcTemplate.update("update  AKTIVITET set OPPFOLGINGSPERIODE_UUID = 'nisse' where VERSJON = " + aktivitet.getVersjon());
        int skalVereEn = jdbcTemplate.queryForObject("select count(*) from  AKTIVITET a1 where GJELDENDE = 1 and exists(select * from AKTIVITET a2 where a1.AKTIVITET_ID = a2.AKTIVITET_ID and a1.OPPFOLGINGSPERIODE_UUID != a2.OPPFOLGINGSPERIODE_UUID)", Integer.class);
        assertEquals(1 ,skalVereEn);
        jdbcTemplate.update("update AKTIVITETJOBB set AKTIVITETJOBB.maks_id = (select max(AKTIVITET_ID) from aktivitet)");
        oppfolgingsperiodeCron.sammkjorOppfolignsperiode();
        
        int skalVereNull = jdbcTemplate.queryForObject("select count(*) from  AKTIVITET a1 where GJELDENDE = 1 and exists(select * from AKTIVITET a2 where a1.AKTIVITET_ID = a2.AKTIVITET_ID and a1.OPPFOLGINGSPERIODE_UUID != a2.OPPFOLGINGSPERIODE_UUID)", Integer.class);
        assertEquals(0, skalVereNull);
    }
}
