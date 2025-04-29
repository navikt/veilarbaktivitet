package no.nav.veilarbaktivitet.veilarbportefolje.dto;

import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


class AktivitetTypeDTOTestContainer {

    @Nested
    class DomainAktivitetsTyperTest {
        static Stream<no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO> domainAktivitetetTyper() {
            return Stream.of(no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO.values());
        }

        @ParameterizedTest
        @MethodSource("domainAktivitetetTyper")
        void mapAlleAktivitetsTyper(no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO aktivitetsType) {
            AktivitetTypeDTO aktivitetTypeDTO = AktivitetTypeDTO.fromDomainAktivitetType(aktivitetsType);
            assertThat(aktivitetTypeDTO).isNotNull();
        }
    }

    @Nested
    class EksternAktivitetstypeTest {
        @Test
        void mapEksterneAktiviteterTilTiltak() {
            AktivitetTypeDTO aktivitetTypeDTO = AktivitetTypeDTO.fromDomainAktivitetType(no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO.EKSTERNAKTIVITET);
            assertThat(aktivitetTypeDTO).isEqualTo(AktivitetTypeDTO.TILTAK);
        }
    }

    @Nested
    class AktivitetskortTilArenaTiltakskoderTest {
        static Stream<AktivitetskortType> aktivitetskortTyper() {
            return Arrays.stream(AktivitetskortType.values()).filter(type -> type != AktivitetskortType.REKRUTTERINGSTREFF);
        }

        @ParameterizedTest
        @MethodSource("aktivitetskortTyper")
        void mapEksterneAktiviteterTilTiltak(AktivitetskortType aktivitetskortType) {
            String arenaTiltakskode = AktivitetTypeDTO.aktivitetsKortTypeToArenaTiltakskode(aktivitetskortType);
            if (aktivitetskortType == AktivitetskortType.ARENA_TILTAK) {
                assertThat(arenaTiltakskode).isNull();
            } else {
                assertThat(arenaTiltakskode).isNotNull();
            }
        }
    }

}