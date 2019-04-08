package no.nav.fo;

import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.dialogarena.aktor.AktorConfig;
import no.nav.fasit.FasitUtils;
import no.nav.fo.veilarbaktivitet.ApplicationContext;
import no.nav.fo.veilarbaktivitet.client.KvpClient;
import no.nav.fo.veilarbaktivitet.config.AbacConfig;
import no.nav.fo.veilarbaktivitet.domain.Person;
import no.nav.fo.veilarbaktivitet.domain.Person.AktorId;
import no.nav.fo.veilarbaktivitet.service.BrukerService;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.test.FasitAssumption;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.context.annotation.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Optional;

import static java.lang.System.setProperty;
import static no.nav.fo.veilarbaktivitet.ApplicationContext.VEILARBOPPFOLGINGAPI_URL_PROPERTY;
import static no.nav.fo.veilarbaktivitet.ApplicationContext.VIRKSOMHET_TILTAKOGAKTIVITET_V1_ENDPOINTURL_PROPERTY;
import static no.nav.fo.veilarbaktivitet.domain.Person.navIdent;
import static org.mockito.Mockito.*;

public abstract class IntegrasjonsTestMedPepOgBrukerServiceMock extends AbstractIntegrasjonsTest {

    protected static final String INNLOGGET_NAV_IDENT = "Z999999";

    @BeforeAll
    @BeforeClass
    public static void setupContext() {
        FasitAssumption.assumeFasitAccessible();
        Assume.assumeFalse(FasitUtils.usingMock());

        setProperty(VIRKSOMHET_TILTAKOGAKTIVITET_V1_ENDPOINTURL_PROPERTY, "https://localhost");
        setProperty(VEILARBOPPFOLGINGAPI_URL_PROPERTY, "https://localhost");
        setupContext(
                IntegrasjonsTestMedPepOgBrukerServiceMock.Config.class,
                IntegrasjonsTest.Request.class,
                AktorConfig.class
        );
    }

    @Configuration
    @EnableTransactionManagement
    @EnableAspectJAutoProxy
    @ComponentScan(basePackages = "no.nav.fo.veilarbaktivitet", excludeFilters = {
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = ApplicationContext.class),
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = AbacConfig.class),
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = BrukerService.class)
    })
    public static class Config {

        @Bean
        public Pep pep() {
            return mock(Pep.class);
        }

        @Bean
        public VeilarbAbacPepClient pepClient() {
            return mock(VeilarbAbacPepClient.class);
        }

        @Bean
        public KvpClient kvpClient() {
            return mock(KvpClient.class);
        }

        @Bean
        public UnleashService unleashService() {
            return mock(UnleashService.class);
        }

        @Bean
        public SystemUserTokenProvider systemUserTokenProvider() {
            return mock(SystemUserTokenProvider.class);
        }

        @Bean
        public BrukerService brukerService() {
            BrukerService brukerService = mock(BrukerService.class);
            when(brukerService.getLoggedInnUser()).thenReturn(Optional.of(navIdent(INNLOGGET_NAV_IDENT)));
            when(brukerService.getAktorIdForPerson(any(Person.class))).thenReturn(Optional.of(TestData.KJENT_AKTOR_ID));
            when(brukerService.getFNRForAktorId(any(AktorId.class))).thenReturn(Optional.of(TestData.KJENT_IDENT));
            return brukerService;
        }
    }

}
