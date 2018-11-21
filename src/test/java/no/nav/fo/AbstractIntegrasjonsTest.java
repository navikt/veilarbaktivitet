package no.nav.fo;

import lombok.SneakyThrows;
import no.nav.dialogarena.config.DevelopmentSecurity;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.jupiter.api.AfterAll;
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
        DevelopmentSecurity.setupIntegrationTestSecurity(new DevelopmentSecurity.IntegrationTestConfig(APPLICATION_NAME));

        annotationConfigApplicationContext = new AnnotationConfigApplicationContext(classes);
        annotationConfigApplicationContext.start();
        platformTransactionManager = getBean(PlatformTransactionManager.class);
    }

    private static final SimpleNamingContextBuilder SIMPLE_NAMING_CONTEXT_BUILDER = new SimpleNamingContextBuilder();

    @Component
    public static class JndiBean {

        public JndiBean() throws Exception {
            SIMPLE_NAMING_CONTEXT_BUILDER.bind(AKTIVITET_DATA_SOURCE_JDNI_NAME, DatabaseTestContext.buildDataSource());
            SIMPLE_NAMING_CONTEXT_BUILDER.activate();
        }

    }

    @BeforeEach
    @Before
    public final void fiksJndiOgLdapKonflikt() throws NamingException {
        SIMPLE_NAMING_CONTEXT_BUILDER.deactivate();
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
        if (platformTransactionManager != null && transactionStatus != null) {
            platformTransactionManager.rollback(transactionStatus);
        }
    }

    @Component
    public static class Request extends MockHttpServletRequest {
    }

    protected static <T> T getBean(Class<T> requiredType) {
        return annotationConfigApplicationContext.getBean(requiredType);
    }

    @AfterAll
    @AfterClass
    public static void close() {
        if (annotationConfigApplicationContext != null) {
            annotationConfigApplicationContext.stop();
            annotationConfigApplicationContext.close();
            annotationConfigApplicationContext.destroy();
            annotationConfigApplicationContext = null;
        }
    }


}
