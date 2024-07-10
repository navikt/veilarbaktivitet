package no.nav.veilarbaktivitet.internapi;

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.aktivitet.domain.StillingsoekEtikettData;
import no.nav.veilarbaktivitet.internapi.model.Aktivitet;
import no.nav.veilarbaktivitet.stilling_fra_nav.Soknadsstatus;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static no.nav.veilarbaktivitet.internapi.InternAktivitetMapperKt.mapTilAktivitet;

class InternAktivitetMapperTestContainer {
    static class StillingFraNavMapperTest {
        static Stream<Soknadsstatus> soknadsstatuser() {
            return Stream.of(Soknadsstatus.values());
        }

        @ParameterizedTest
        @MethodSource("soknadsstatuser")
        void stillingFraNavSoknadsstatuser(Soknadsstatus soknadsstatus) {
            AktivitetData aktivitetData = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.STILLING_FRA_NAV);
            aktivitetData.getStillingFraNavData().setSoknadsstatus(soknadsstatus);
            Aktivitet aktivitet = mapTilAktivitet(aktivitetData);
            Assertions.assertThat(aktivitet).isNotNull();
        }
    }

    static class JobbsoekingMapperTest {
        static Stream<StillingsoekEtikettData> stillingsoekEtiketter() {
            return Stream.of(StillingsoekEtikettData.values());
        }

        @ParameterizedTest
        @MethodSource("stillingsoekEtiketter")
        void jobbsoekingEtiketter(StillingsoekEtikettData stillingsoekEtikett) {
            AktivitetData aktivitetData = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.JOBBSOEKING);
            aktivitetData.getStillingsSoekAktivitetData().withStillingsoekEtikett(stillingsoekEtikett);
            Aktivitet aktivitet = mapTilAktivitet(aktivitetData);
            Assertions.assertThat(aktivitet).isNotNull();
        }
    }
}
