package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.EksternAktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitet.domain.Ident;
import no.nav.veilarbaktivitet.person.Innsender;
import no.nav.veilarbaktivitet.person.Person;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import static no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortMapper.mapTilAktivitetData;
import static no.nav.veilarbaktivitet.aktivitetskort.dto.IdentType.ARENAIDENT;
import static org.assertj.core.api.Assertions.assertThat;

class AktivitetskortMapperTest {

    Aktivitetskort aktivitetskort() {
        return Aktivitetskort.builder()
                .id(UUID.randomUUID())
                .personIdent("1234567890")
                .startDato(LocalDate.now().minusDays(30))
                .sluttDato(LocalDate.now().minusDays(30))
                .tittel("The Elder Scrolls: Arena")
                .beskrivelse("arenabeskrivelse")
                .aktivitetStatus(AktivitetStatus.GJENNOMFORES)
                .endretAv(new Ident("arenaEndretav", Innsender.ARENAIDENT))
                .endretTidspunkt(ZonedDateTime.now())
                .build();
    }

    @Test
    public void should_map_list_fields_to_empty_list_if_they_are_null() {
        // These fields are set to null when deserializing, but are empty lists when using builder
        var aktivitetskortWithNullFields = aktivitetskort()
                .withEtiketter(null)
                .withHandlinger(null)
                .withDetaljer(null);

        var result = mapTilAktivitetData(
                new EksternAktivitetskortBestilling(
                        aktivitetskortWithNullFields,
                        "test-source",
                        AktivitetskortType.ARENA_TILTAK,
                        UUID.randomUUID(),
                        ActionType.UPSERT_AKTIVITETSKORT_V1,
                        Person.aktorId("1234567890")
                ),
                ZonedDateTime.now()
        );
        assertThat(result.getEksternAktivitetData().getDetaljer()).isEmpty();
        assertThat(result.getEksternAktivitetData().getEtiketter()).isEmpty();
        assertThat(result.getEksternAktivitetData().getHandlinger()).isEmpty();
    }
}