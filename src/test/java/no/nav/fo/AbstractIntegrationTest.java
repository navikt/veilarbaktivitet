package no.nav.fo;

import no.nav.veilarbaktivitet.db.testdriver.TestDriver;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import static no.nav.sbl.dialogarena.test.SystemProperties.setTemporaryProperty;
import static no.nav.veilarbaktivitet.db.DatabaseContext.migrateDatabase;

public abstract class AbstractIntegrationTest {

    protected static AnnotationConfigApplicationContext annotationConfigApplicationContext;
    private static TransactionStatus transaction;
    private static PlatformTransactionManager platformTransactionManager;

    public static void setupContext(Class<?>... classes) {
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
    public void beginTransaction() {
        transaction = platformTransactionManager.getTransaction(new TransactionTemplate());
    }

    @AfterEach
    @After
    public void rollbackTransaction() {
        platformTransactionManager.rollback(transaction);
        transaction = null;
    }

    @AfterAll
    @AfterClass
    public static void stopSpringContext() {
        if (annotationConfigApplicationContext != null) {
            annotationConfigApplicationContext.stop();
        }
    }

    protected static <T> T getBean(Class<T> requiredType) {
        return annotationConfigApplicationContext.getBean(requiredType);
    }
}
