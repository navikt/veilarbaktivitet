package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.common.featuretoggle.UnleashClient;
import no.nav.veilarbaktivitet.aktivitetskort.test.AktivitetskortTestMetrikker;
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO;
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingV2Client;
import no.nav.veilarbaktivitet.person.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.time.LocalDateTime;
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

        migreringService = new MigreringService(mock(UnleashClient.class), oppfolgingV2Client, mock(AktivitetskortTestMetrikker.class));
    }

    private static final Person.AktorId AKTOR_ID = Person.aktorId("1234");
    private static final ZonedDateTime DATE_TIME = ZonedDateTime.now();
    private static final LocalDateTime LOCAL_DATE_TIME = DATE_TIME.toLocalDateTime();

    @Test
    void opprettetTidspunkt_passer_i_gammel_periode() {
        var riktigPeriode = oppfperiodeDTO(DATE_TIME.minusDays(30), DATE_TIME.minusDays(20));
        var perioder = List.of(
                riktigPeriode,
                oppfperiodeDTO(DATE_TIME.minusDays(10), null)
        );

        OppfolgingPeriodeMinimalDTO oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME.minusDays(25));

        assertThat(oppfolgingsperiode.getUuid()).isEqualTo(riktigPeriode.getUuid());
    }

    @Test
    void opprettetTidspunkt_passer_i_gjeldende_periode() {
        var riktigPeriode = oppfperiodeDTO(DATE_TIME.minusDays(10), null);
        var perioder = List.of(
                oppfperiodeDTO(DATE_TIME.minusDays(30), DATE_TIME.minusDays(20)),
                riktigPeriode
        );

        OppfolgingPeriodeMinimalDTO oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME.minusHours(10));

        assertThat(oppfolgingsperiode.getUuid()).isEqualTo(riktigPeriode.getUuid());
    }

    @Test
    void opprettetTidspunkt_passer_paa_startDato() {
        var riktigPeriode = oppfperiodeDTO(DATE_TIME.minusDays(10), null);
        var perioder = List.of(
                oppfperiodeDTO(DATE_TIME.minusDays(30), DATE_TIME.minusDays(20)),
                riktigPeriode
        );

        OppfolgingPeriodeMinimalDTO oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME.minusDays(10));

        assertThat(oppfolgingsperiode.getUuid()).isEqualTo(riktigPeriode.getUuid());
    }

    @Test
    void opprettetTidspunkt_er_like_langt_unna_to_startdatoer() {
        var riktigPeriode = oppfperiodeDTO(DATE_TIME.minusDays(10), DATE_TIME.minusDays(0));
        var perioder1 = List.of(
                oppfperiodeDTO(DATE_TIME.minusDays(20), DATE_TIME.minusDays(18)),
                riktigPeriode
        );

        OppfolgingPeriodeMinimalDTO oppfolgingsperiode1 = stubOgFinnOppgolgingsperiode(perioder1, LOCAL_DATE_TIME.minusDays(15));

        assertThat(oppfolgingsperiode1).isNull();

        // skal være kommutativ
        var perioder2 = List.of(
                riktigPeriode,
                oppfperiodeDTO(DATE_TIME.minusDays(20), DATE_TIME.minusDays(18))
        );
        OppfolgingPeriodeMinimalDTO oppfolgingsperiode2 = stubOgFinnOppgolgingsperiode(perioder2, LOCAL_DATE_TIME.minusDays(15));

        assertThat(oppfolgingsperiode2).isNull();
    }

    @Test
    void opprettetTidspunkt_i_to_gamle_perioder() {
        // Er riktig fordi den er "nyere" enn den andre perioden
        var riktigPeriode = oppfperiodeDTO(DATE_TIME.minusDays(16), DATE_TIME.minusDays(5));
        var perioder = List.of(
                oppfperiodeDTO(DATE_TIME.minusDays(20), DATE_TIME.minusDays(10)),
                riktigPeriode
        );

        OppfolgingPeriodeMinimalDTO oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME.minusDays(15));

        assertThat(oppfolgingsperiode.getUuid()).isEqualTo(riktigPeriode.getUuid());
    }

    @Test
    void opprettetTidspunkt_i_en_gammel_og_en_gjeldende_periode() {
        var riktigPeriode = oppfperiodeDTO(DATE_TIME.minusDays(16), DATE_TIME);
        var perioder = List.of(
                oppfperiodeDTO(DATE_TIME.minusDays(20), DATE_TIME.minusDays(10)),
                riktigPeriode
        );

        OppfolgingPeriodeMinimalDTO oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME.minusDays(15));

        assertThat(oppfolgingsperiode.getUuid()).isEqualTo(riktigPeriode.getUuid());
    }

    @Test
    void opprettetTidspunkt_mot_en_bruker_som_ikke_har_oppfolgingsperioder() {
        List<OppfolgingPeriodeMinimalDTO> perioder = List.of();

        OppfolgingPeriodeMinimalDTO oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME.minusDays(15));

        assertThat(oppfolgingsperiode).isNull();
    }

    @Test
    void velg_naermeste_periode_etter_opprettetitdspunkt_OG_som_er_10_min_innen_opprettetTidspunkt() {
        var riktigPeriode = oppfperiodeDTO(DATE_TIME.minusDays(10).plusMinutes(5), DATE_TIME);
        var perioder = List.of(
                oppfperiodeDTO(DATE_TIME.minusDays(10).minusMinutes(4), DATE_TIME.minusDays(10).minusMinutes(2)),
                riktigPeriode
        );

        OppfolgingPeriodeMinimalDTO oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME.minusDays(10));

        assertThat(oppfolgingsperiode.getUuid()).isEqualTo(riktigPeriode.getUuid());
    }

    @Test
    void ikke_velg_periode_hvis_perioden_slutter_foer_aktivitetens_opprettetTidspunkt() {
        var riktigPeriode = oppfperiodeDTO(DATE_TIME.minusDays(10).minusMinutes(5), DATE_TIME.minusDays(10).minusMinutes(2));
        var perioder = List.of(
                riktigPeriode
        );

        OppfolgingPeriodeMinimalDTO oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME.minusDays(10));

        assertThat(oppfolgingsperiode).isNull();
    }

    private OppfolgingPeriodeMinimalDTO stubOgFinnOppgolgingsperiode(List<OppfolgingPeriodeMinimalDTO> perioder, LocalDateTime opprettetTidspunkt) {
        when(oppfolgingV2Client.hentOppfolgingsperioder(ArgumentMatchers.any())).thenReturn(Optional.of(perioder));
        Optional<OppfolgingPeriodeMinimalDTO> oppfolgingsperiode = migreringService.finnOppfolgingsperiode(AKTOR_ID, opprettetTidspunkt, null, null);

        return oppfolgingsperiode.orElse(null);
    }

    private OppfolgingPeriodeMinimalDTO oppfperiodeDTO(ZonedDateTime startDato, ZonedDateTime sluttDato) {
        return OppfolgingPeriodeMinimalDTO.builder()
                .uuid(UUID.randomUUID())
                .startDato(startDato)
                .sluttDato(sluttDato)
                .build();
    }
}
