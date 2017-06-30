package no.nav.fo;

import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarbaktivitet.ApplicationContext;
import no.nav.fo.veilarbaktivitet.config.AbacConfig;
import no.nav.fo.veilarbaktivitet.service.TiltakOgAktivitetMock;
import no.nav.modig.testcertificates.TestCertificates;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.context.annotation.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.IOException;

import static org.mockito.Mockito.mock;

public abstract class IntegrasjonsTestUtenArenaMock extends AbstractIntegrasjonsTest {

    @BeforeClass
    public static void testCertificates() throws IOException {
        TestCertificates.setupKeyAndTrustStore();
    }

    @BeforeAll
    @BeforeClass
    public static void setupContext() {
        setupContext(
                IntegrasjonsTestUtenArenaMock.Config.class,
                IntegrasjonsTest.JndiBean.class,
                IntegrasjonsTest.Request.class
        );
    }

    @Configuration
    @EnableTransactionManagement
    @EnableAspectJAutoProxy
    @ComponentScan(basePackages = "no.nav.fo.veilarbaktivitet", excludeFilters = {
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = ApplicationContext.class),
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = TiltakOgAktivitetMock.class),
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = AbacConfig.class),
    })
    public static class Config {

        @Bean
        public PepClient pepClient(){
            return mock(PepClient.class);
        }

    }

}
