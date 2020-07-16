package no.nav.veilarbaktivitet.db;

import no.nav.fo.AbstractIntegrationTest;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.db.dao.AktivitetFeedDAO;
import no.nav.veilarbaktivitet.db.dao.AvsluttetOppfolgingFeedDAO;
import no.nav.veilarbaktivitet.db.dao.KVPFeedDAO;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;

public abstract class DatabaseTest extends AbstractIntegrationTest {
    @BeforeAll
    @BeforeClass
    public static void setup() {
        setupContext(
                DatabaseContext.class,
                Database.class,
                AktivitetDAO.class,
                AktivitetFeedDAO.class,
                AvsluttetOppfolgingFeedDAO.class,
                KVPFeedDAO.class
        );

    }
}
