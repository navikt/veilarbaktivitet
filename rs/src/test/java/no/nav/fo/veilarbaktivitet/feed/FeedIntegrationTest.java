package no.nav.fo.veilarbaktivitet.feed;

import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.feed.FeedProducerTester;
import no.nav.fo.feed.controller.FeedController;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData;
import org.junit.jupiter.api.BeforeEach;

import javax.inject.Inject;
import java.util.Date;

import static no.nav.fo.veilarbaktivitet.AktivitetDataTestBuilder.nyAktivitet;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.ISO8601FromDate;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.dateFromISO8601;

public class FeedIntegrationTest extends IntegrasjonsTest implements FeedProducerTester {

    private static long counter;

    @Inject
    private FeedController feedController;

    @Inject
    private AktivitetDAO aktivitetDAO;

    @BeforeEach
    public void setup() {
        System.setProperty("aktiviteter.feed.brukertilgang", SubjectHandler.getSubjectHandler().getUid());
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