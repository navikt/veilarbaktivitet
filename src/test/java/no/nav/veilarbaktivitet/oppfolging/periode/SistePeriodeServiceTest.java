package no.nav.veilarbaktivitet.oppfolging.periode;


import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.person.Person;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SistePeriodeServiceTest extends SpringBootTestBase {
    @Autowired
    SistePeriodeDAO sistePeriodeDAO;
    @Autowired
    SistePeriodeService sistePeriodeService;

    @Test
    void skalHenteOgBrukeSistePeriodeFraDao() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        UUID oppfolgingsperiodeId = UUID.randomUUID();
        Oppfolgingsperiode oppfolgingsperiode = new Oppfolgingsperiode(mockBruker.getAktorId().get(), oppfolgingsperiodeId, ZonedDateTime.now().minusDays(5), null);
        sistePeriodeDAO.uppsertOppfolingsperide(oppfolgingsperiode);
        UUID fraBasen = sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(mockBruker.getAktorIdAsAktorId());
        assertThat(fraBasen).isEqualTo(oppfolgingsperiodeId);
    }

    @Test
    void skalHandtereFlereLikePerioder() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        UUID oppfolgingsperiodeId = UUID.randomUUID();
        Oppfolgingsperiode oppfolgingsperiode = new Oppfolgingsperiode(mockBruker.getAktorId().get(), oppfolgingsperiodeId, ZonedDateTime.now().minusDays(5), null);
        sistePeriodeDAO.uppsertOppfolingsperide(oppfolgingsperiode);
        sistePeriodeDAO.uppsertOppfolingsperide(oppfolgingsperiode);
        UUID fraBasen = sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(mockBruker.getAktorIdAsAktorId());
        assertThat(fraBasen).isEqualTo(oppfolgingsperiodeId);
    }

    @Test
    void skalHoppeOverGammelPeriode() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        UUID oppfolgingsperiodeId = UUID.randomUUID();
        Oppfolgingsperiode oppfolgingsperiode = new Oppfolgingsperiode(mockBruker.getAktorId().get(), oppfolgingsperiodeId, ZonedDateTime.now().minusDays(5), null);
        Oppfolgingsperiode gammelPeriode = new Oppfolgingsperiode(mockBruker.getAktorId().get(), UUID.randomUUID(), ZonedDateTime.now().minusDays(10), null);
        sistePeriodeDAO.uppsertOppfolingsperide(oppfolgingsperiode);
        sistePeriodeDAO.uppsertOppfolingsperide(gammelPeriode);
        UUID fraBasen = sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(mockBruker.getAktorIdAsAktorId());
        assertThat(fraBasen).isEqualTo(oppfolgingsperiodeId);
    }

    @Test
    void fallBackHvisPeriodeAvsluttet() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        UUID oppfolgingsperiodeId = UUID.randomUUID();
        Oppfolgingsperiode avsluttet = new Oppfolgingsperiode(mockBruker.getAktorId().get(), oppfolgingsperiodeId, ZonedDateTime.now().minusDays(5), ZonedDateTime.now());
        sistePeriodeDAO.uppsertOppfolingsperide(avsluttet);
        UUID fraVeilarbOppfolging = sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(mockBruker.getAktorIdAsAktorId());
        assertThat(fraVeilarbOppfolging).isEqualTo(mockBruker.getOppfolgingsperiodeId());

    }

    @Test
    void oppfolgingFeiler() {
        BrukerOptions oppfolgingFeiler = BrukerOptions.happyBrukerBuilder().oppfolgingFeiler(true).build();
        MockBruker bruker = MockNavService.createBruker(oppfolgingFeiler);
        Person.AktorId aktorId = bruker.getAktorIdAsAktorId();
        assertThrows(ResponseStatusException.class, () -> sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(aktorId));
    }

    @Test
    void ikkeUnderOppfolging() {
        BrukerOptions ikkeUnderOppfolging = BrukerOptions.happyBrukerBuilder().underOppfolging(false).build();
        MockBruker bruker = MockNavService.createBruker(ikkeUnderOppfolging);
        Person.AktorId aktorId = bruker.getAktorIdAsAktorId();
        assertThrows(IngenGjeldendePeriodeException.class, () -> sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(aktorId));
    }


}
