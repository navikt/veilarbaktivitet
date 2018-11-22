package no.nav.fo;

import lombok.SneakyThrows;
import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.fo.veilarbaktivitet.db.testdriver.TestDriver;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;

import static no.nav.fo.veilarbaktivitet.TestConfig.APPLICATION_NAME;
import static no.nav.fo.veilarbaktivitet.db.DatabaseContext.*;
import static no.nav.sbl.dialogarena.test.SystemProperties.setTemporaryProperty;

public abstract class AbstractIntegrasjonsTest {

    private static AnnotationConfigApplicationContext annotationConfigApplicationContext;
    private static PlatformTransactionManager platformTransactionManager;
    private TransactionStatus transactionStatus;

    @SneakyThrows
    public static void setupContext(Class<?>... classes) {
        DevelopmentSecurity.setupIntegrationTestSecurity(new DevelopmentSecurity.IntegrationTestConfig(APPLICATION_NAME));

        setTemporaryProperty(VEILARBAKTIVITETDATASOURCE_URL_PROPERTY, TestDriver.getURL(), () -> {
            setTemporaryProperty(VEILARBAKTIVITETDATASOURCE_USERNAME_PROPERTY, "sa", () -> {
                setTemporaryProperty(VEILARBAKTIVITETDATASOURCE_PASSWORD_PROPERTY, "pw", () -> {

                    annotationConfigApplicationContext = new AnnotationConfigApplicationContext(classes);
                    annotationConfigApplicationContext.start();
                    platformTransactionManager = getBean(PlatformTransactionManager.class);

                    migrateDatabase(getBean(DataSource.class));
                });
            });
        });
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
