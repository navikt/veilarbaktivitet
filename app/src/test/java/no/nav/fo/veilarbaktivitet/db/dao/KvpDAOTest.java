package no.nav.fo.veilarbaktivitet.db.dao;

import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbaktivitet.db.Database;
import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

public class KvpDAOTest extends IntegrasjonsTest {
    @Inject
    private KvpDAO repository;

    @Inject
    private Database db;

    /**
     * Work around poor data isolation between tests.
     */
    @BeforeEach
    public void truncate() {
        db.update("DELETE FROM KVP");
    }


    @Test
    public void testCurrentSerial() {
        final long SERIAL_START = 398;

        for (int i = 0; i < 100; i++) {
            KvpDTO k = new KvpDTO()
                    .setKvpId(i+1)
                    .setAktorId("Z999999")
                    .setSerial(SERIAL_START - i)
                    .setEnhet("1337");
            repository.insert(k);
        }
        long currentSerial = repository.currentSerial();
        assertThat(SERIAL_START, equalTo(currentSerial));
    }
}
