package no.nav.fo;

import no.nav.fo.veilarbaktivitet.ApplicationContext;
import no.nav.fo.veilarbaktivitet.service.TiltakOgAktivitetMock;
import org.junit.BeforeClass;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@ContextConfiguration(classes = {
        IntegrasjonsTestUtenArenaMock.Config.class,
        IntegrasjonsTest.JndiBean.class,
        IntegrasjonsTest.Request.class
})
public abstract class IntegrasjonsTestUtenArenaMock extends AbstractIntegrasjonsTest {

    @BeforeClass
    public static void setupSts() {
        System.setProperty("no.nav.modig.security.sts.url", "/");
    }

    @Configuration
    @EnableTransactionManagement
    @EnableAspectJAutoProxy
    @ComponentScan(basePackages = "no.nav.fo.veilarbaktivitet", excludeFilters = {
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = ApplicationContext.class),
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = TiltakOgAktivitetMock.class),
    })
    public static class Config {}

}
