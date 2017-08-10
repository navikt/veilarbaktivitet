package no.nav.fo;

import lombok.SneakyThrows;
import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.dialogarena.config.fasit.FasitUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.naming.NamingException;

import static no.nav.fo.veilarbaktivitet.TestConfig.APPLICATION_NAME;
import static no.nav.fo.veilarbaktivitet.db.DatabaseContext.AKTIVITET_DATA_SOURCE_JDNI_NAME;

public abstract class AbstractIntegrasjonsTest {

    private static AnnotationConfigApplicationContext annotationConfigApplicationContext;
    private static PlatformTransactionManager platformTransactionManager;
    private TransactionStatus transactionStatus;

    @SneakyThrows
    public static void setupContext(Class<?>... classes) {
        DevelopmentSecurity.setupIntegrationTestSecurity(FasitUtils.getServiceUser("srvveilarbaktivitet", APPLICATION_NAME, "t6"));
        System.getProperties().load(AbstractIntegrasjonsTest.class.getResourceAsStream("/test.properties"));
        System.getProperties().load(AbstractIntegrasjonsTest.class.getResourceAsStream("/integrasjonstest.properties"));
        DevelopmentSecurity.configureLdap(FasitUtils.getLdapConfig("ldap", "veilarbaktivitet", "t6"));

        annotationConfigApplicationContext = new AnnotationConfigApplicationContext(classes);
        annotationConfigApplicationContext.start();
        platformTransactionManager = getBean(PlatformTransactionManager.class);
    }

    @Component
    public static class JndiBean {

        private final SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();

        public JndiBean() throws Exception {
            builder.bind(AKTIVITET_DATA_SOURCE_JDNI_NAME, DatabaseTestContext.buildDataSource());
            builder.activate();
        }

    }

    @BeforeEach
    @Before
    public final void fiksJndiOgLdapKonflikt() throws NamingException {
        getBean(JndiBean.class).builder.deactivate();
    }

    @BeforeEach
    @Before
    public void injectAvhengigheter() {
        annotationConfigApplicationContext.getAutowireCapableBeanFactory().autowireBean(this);
    }

    @BeforeEach
    @Before
    public void startTransaksjon() {
        transactionStatus = platformTransactionManager.getTransaction(new DefaultTransactionDefinition());
    }

    @AfterEach
    @After
    public void rollbackTransaksjon() {
        platformTransactionManager.rollback(transactionStatus);
    }

    @Component
    public static class Request extends MockHttpServletRequest {
    }

    protected static <T> T getBean(Class<T> requiredType) {
        return annotationConfigApplicationContext.getBean(requiredType);
    }


}
