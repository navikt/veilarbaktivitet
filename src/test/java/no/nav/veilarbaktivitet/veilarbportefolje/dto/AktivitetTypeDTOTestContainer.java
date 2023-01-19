package no.nav.veilarbaktivitet.veilarbportefolje.dto;

import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;


class AktivitetTypeDTOTestContainer {

    static class DomainAktivitetsTyperTest {
        static Stream<no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO> domainAktivitetetTyper() {
            return Stream.of(no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO.values());
        }

        @ParameterizedTest
        @MethodSource("domainAktivitetetTyper")
        void mapAlleAktivitetsTyper(no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO aktivitetsType) {
            AktivitetTypeDTO aktivitetTypeDTO = AktivitetTypeDTO.fromDomainAktivitetType(aktivitetsType, AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD);
            Assertions.assertThat(aktivitetTypeDTO).isNotNull();
        }
    }

    static class EksternAktivitetstypeTest {
        static Stream<AktivitetskortType> aktivitetskortTyper() {
            return Stream.of(AktivitetskortType.values());
        }

        @ParameterizedTest
        @MethodSource("aktivitetskortTyper")
        void mapEksterneAktiviteterTilTiltak(AktivitetskortType aktivitetskortType) {
            AktivitetTypeDTO aktivitetTypeDTO = AktivitetTypeDTO.fromDomainAktivitetType(no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO.EKSTERNAKTIVITET, aktivitetskortType);
            Assertions.assertThat(aktivitetTypeDTO).isEqualTo(AktivitetTypeDTO.TILTAK);
        }
    }

}