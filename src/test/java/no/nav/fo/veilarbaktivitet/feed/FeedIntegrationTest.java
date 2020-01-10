package no.nav.fo.veilarbaktivitet.feed;

import no.nav.brukerdialog.security.context.SubjectExtension;
import no.nav.common.auth.Subject;
import no.nav.fo.AbstractIntegrationTest;
import no.nav.fo.feed.FeedProducerTester;
import no.nav.fo.feed.controller.FeedController;
import no.nav.fo.veilarbaktivitet.db.Database;
import no.nav.fo.veilarbaktivitet.db.DatabaseContext;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetFeedDAO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.Date;

import static java.lang.System.setProperty;
import static no.nav.brukerdialog.security.domain.IdentType.InternBruker;
import static no.nav.common.auth.SsoToken.oidcToken;
import static no.nav.fo.veilarbaktivitet.AktivitetDataTestBuilder.nyAktivitet;
import static no.nav.fo.veilarbaktivitet.ApplicationContext.AKTIVITETER_FEED_BRUKERTILGANG_PROPERTY;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.ISO8601FromDate;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.dateFromISO8601;

@ExtendWith(SubjectExtension.class)
public class FeedIntegrationTest extends AbstractIntegrationTest implements FeedProducerTester {

    private static final String TEST_IDENT = FeedIntegrationTest.class.getSimpleName();

    private static long counter;

    @BeforeAll
    @BeforeClass
    public static void init() {
        setupContext(
                DatabaseContext.class,
                Database.class,
                AktivitetDAO.class,
                AktivitetFeedDAO.class,
                FeedController.class
        );

    }

    protected static <T> T getBean(Class<T> requiredType) {
        return annotationConfigApplicationContext.getBean(requiredType);
    }

    @Inject
    private FeedController feedController;

    @Inject
    private AktivitetDAO aktivitetDAO;

    @BeforeEach
    public void setup(SubjectExtension.SubjectStore subjectStore) {
        subjectStore.setSubject(new Subject(TEST_IDENT, InternBruker, oidcToken("token")));
        setProperty(AKTIVITETER_FEED_BRUKERTILGANG_PROPERTY, TEST_IDENT);
    }

    @Override
    public FeedController getFeedController() {
        return feedController;
    }

    @Override
    public void opprettElementForFeed(String feedName, String id) {
        aktivitetDAO.insertAktivitet(nyAktivitet()
                        .aktivitetType(AktivitetTypeData.values()[0])
                        .build(),
                dateFromISO8601(id)
        );
    }

    @Override
    public String unikId(String feedName) {
        return ISO8601FromDate(new Date(counter++));
    }

    @Override
    public String forsteMuligeId(String feedName) {
        return ISO8601FromDate(new Date(0));
    }
}
