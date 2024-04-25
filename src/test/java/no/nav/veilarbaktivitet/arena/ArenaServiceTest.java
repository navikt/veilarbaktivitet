package no.nav.veilarbaktivitet.arena;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitetskort.MigreringService;
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDAO;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.avtalt_med_nav.Forhaandsorientering;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDAO;
import no.nav.veilarbaktivitet.avtalt_med_nav.Type;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeDAO;
import no.nav.veilarbaktivitet.person.PersonService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ArenaServiceTest {

    @Test
    void skal_bruke_nyeste_fho_hvis_det_finnes_flere_på_en_arena_aktivitet() {
        var arenaService = new ArenaService(
                mock(ForhaandsorienteringDAO.class),
                mock(MeterRegistry.class),
                mock(BrukernotifikasjonService.class),
                mock(VeilarbarenaClient.class),
                mock(IdMappingDAO.class),
                mock(PersonService.class),
                mock(AktivitetDAO.class),
                mock(OppfolgingsperiodeDAO.class),
                mock(MigreringService.class)
        );
        var arenaAktivitet = ArenaAktivitetDTO.builder()
                .id("LOL")
                .build();
        var nyesteDato = Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        var gamlesteDato = Date.from(LocalDate.now().minusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant());
        var fhos = List.of(
            Forhaandsorientering.builder()
                .type(Type.SEND_FORHAANDSORIENTERING)
                .opprettetDato(nyesteDato)
                .id("nyeste")
                .arenaAktivitetId(arenaAktivitet.getId())
                .build(),
            Forhaandsorientering.builder()
                .type(Type.SEND_FORHAANDSORIENTERING)
                .opprettetDato(gamlesteDato)
                .id("gamleste")
                .arenaAktivitetId(arenaAktivitet.getId())
                .build()
        );
        var merged = arenaService.mergeMedForhaandsorientering(fhos,  List.of(arenaAktivitet)).get(0);
        assertEquals(merged.getForhaandsorientering().getId(), fhos.get(0).getId());
    }
}