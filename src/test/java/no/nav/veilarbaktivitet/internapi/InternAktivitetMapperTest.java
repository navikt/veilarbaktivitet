package no.nav.veilarbaktivitet.internapi;

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.aktivitet.domain.StillingsoekEtikettData;
import no.nav.veilarbaktivitet.internapi.model.Aktivitet;
import no.nav.veilarbaktivitet.stilling_fra_nav.Soknadsstatus;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class InternAktivitetMapperTest {

    @RunWith(Parameterized.class)
    public static class StillingFraNavMapperTest {

        @Parameter
        public Soknadsstatus soknadsstatus;

        @Parameters(name = "SFN - Soknadsstatus: {0}")
        public static Soknadsstatus[] soknadsstatuser() {
            return Soknadsstatus.values();
        }

        @Test
        public void stillingFraNavSoknadsstatuser() {
            AktivitetData aktivitetData = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.STILLING_FRA_NAV);
            aktivitetData.getStillingFraNavData().setSoknadsstatus(soknadsstatus);
            Aktivitet aktivitet = InternAktivitetMapper.mapTilAktivitet(aktivitetData);
            Assertions.assertThat(aktivitet).isNotNull();
        }
    }

    @RunWith(Parameterized.class)
    public static class JobbsoekingMapperTest {

        @Parameter
        public StillingsoekEtikettData stillingsoekEtikett;

        @Parameters(name = "Jobbsoeking - Etikett: {0}")
        public static StillingsoekEtikettData[] stillingsoekEtiketter() {
            return StillingsoekEtikettData.values();
        }

        @Test
        public void jobbsoekingEtiketter() {
            AktivitetData aktivitetData = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.JOBBSOEKING);
            aktivitetData.getStillingsSoekAktivitetData().withStillingsoekEtikett(stillingsoekEtikett);
            Aktivitet aktivitet = InternAktivitetMapper.mapTilAktivitet(aktivitetData);
            Assertions.assertThat(aktivitet).isNotNull();
        }
    }
}
