package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.val;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO;
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingV2Client;
import no.nav.veilarbaktivitet.person.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// la denne stå til vi har tatt inn tiltaksativitet, utdanningsaktivitet og gruppeaktivitet
class MigreringServiceTest {

    private MigreringService migreringService;

    private OppfolgingV2Client oppfolgingV2Client;

    @BeforeEach
    public void setup() {
        oppfolgingV2Client = mock(OppfolgingV2Client.class);

        migreringService = new MigreringService(mock(UnleashClient.class), oppfolgingV2Client);
    }

    /* note
        case: opprettetTidspunkt I en gammel periode
        case: opprettetTidspunkt I en gjeldende periode
        case: opprettetTidspunkt PÅ en startdato
        case: opprettetTidspunkt der abs(opprettetTidspunkt - periode1.startDato) == abs(opprettetTidspunkt - periode2.startDato). nb: skal velge den som har startDato ETTER opprettetTidspunkt
        case: opprettetTidspunkt I to gamle perioder
        case: opprettetTidspunkt I to perioder - en gammel og en gjeldende
        case: opprettetTidspunkt mot en bruker som IKKE har noen oppfolgingsperioder
     */

    private static final Person.AktorId AKTOR_ID = Person.aktorId("1234");
    private static final ZonedDateTime DATE_TIME = ZonedDateTime.of(2022, 10, 20, 12, 30, 0, 0, ZoneId.of("UTC"));
    private static final LocalDateTime LOCAL_DATE_TIME = DATE_TIME.toLocalDateTime();

    @Test
    void opprettetTidspunkt_passer_i_gammel_periode() {
        var riktigPeriode = oppfPeriodeDTO(DATE_TIME.minusDays(30), DATE_TIME.minusDays(20));
        var perioder = List.of(
                riktigPeriode,
                oppfPeriodeDTO(DATE_TIME.minusDays(10), null)
        );

        OppfolgingPeriodeMinimalDTO oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME.minusDays(25));

        assertThat(oppfolgingsperiode.getUuid()).isEqualTo(riktigPeriode.getUuid());
    }

    @Test
    void opprettetTidspunkt_passer_i_gjeldende_periode() {
        var riktigPeriode = oppfPeriodeDTO(DATE_TIME.minusDays(10), null);
        var perioder = List.of(
                oppfPeriodeDTO(DATE_TIME.minusDays(30), DATE_TIME.minusDays(20)),
                riktigPeriode
        );

        OppfolgingPeriodeMinimalDTO oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME.minusHours(10));

        assertThat(oppfolgingsperiode.getUuid()).isEqualTo(riktigPeriode.getUuid());
    }

    @Test
    void opprettetTidspunkt_passer_paa_startDato() {
        var riktigPeriode = oppfPeriodeDTO(DATE_TIME.minusDays(10), null);
        var perioder = List.of(
                oppfPeriodeDTO(DATE_TIME.minusDays(30), DATE_TIME.minusDays(20)),
                riktigPeriode
        );

        OppfolgingPeriodeMinimalDTO oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME.minusDays(10));

        assertThat(oppfolgingsperiode.getUuid()).isEqualTo(riktigPeriode.getUuid());
    }

    @Test
    void opprettetTidspunkt_asd() {
        var riktigPeriode = oppfPeriodeDTO(DATE_TIME.minusDays(10), DATE_TIME.minusDays(0));
        var perioder = List.of(
                oppfPeriodeDTO(DATE_TIME.minusDays(20), DATE_TIME.minusDays(18)),
                riktigPeriode
        );

        OppfolgingPeriodeMinimalDTO oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME.minusDays(15));

        assertThat(oppfolgingsperiode.getUuid()).isEqualTo(riktigPeriode.getUuid());
    }

    private OppfolgingPeriodeMinimalDTO stubOgFinnOppgolgingsperiode(List<OppfolgingPeriodeMinimalDTO> perioder, LocalDateTime opprettetTidspunkt) {
        when(oppfolgingV2Client.hentOppfolgingsperioder(ArgumentMatchers.any())).thenReturn(Optional.of(perioder));
        Optional<OppfolgingPeriodeMinimalDTO> oppfolgingsperiode = migreringService.finnOppfolgingsperiode(AKTOR_ID, opprettetTidspunkt);

        return oppfolgingsperiode.orElse(null);
    }

    private OppfolgingPeriodeMinimalDTO oppfPeriodeDTO(ZonedDateTime startDato, ZonedDateTime sluttDato) {
        return OppfolgingPeriodeMinimalDTO.builder()
                .uuid(UUID.randomUUID())
                .startDato(startDato)
                .sluttDato(sluttDato)
                .build();
    }
}
