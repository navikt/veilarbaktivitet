package no.nav.fo.veilarbaktivitet.db.dao;

import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbaktivitet.db.Database;
import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class KvpDAOTest extends IntegrasjonsTest {

    @Inject
    private KvpDAO repository;

    @Inject
    private Database db;

    private static final long SERIAL_START = 398;
    private static final String ENHET_ID = "1337";
    private static final String AKTOR_ID = "***REMOVED***";

    /**
     * Work around poor data isolation between tests.
     */
    @BeforeEach
    public void truncate() {
        db.update("DELETE FROM KVP");
    }

    private KvpDTO baseDTO() {
        return new KvpDTO().setAktorId(AKTOR_ID).setEnhet(ENHET_ID);
    }

    /**
     * Test that currentSerial() always returns the highest serial.
     */
    @Test
    public void testCurrentSerial() {
        for (int i = 0; i < 100; i++) {
            KvpDTO k = baseDTO()
                    .setKvpId(i+1)
                    .setSerial(SERIAL_START - i)
                    .setOpprettetDato(new Date());
            repository.insert(k);
            long currentSerial = repository.currentSerial();
            assertThat(SERIAL_START, equalTo(currentSerial));
        }
    }

    /**
     * Test that enhetID() returns the correct office ID based on rows in the KVP table.
     */
    @Test
    public void testEnhetID() {
        String enhetID;
        KvpDTO k;
        Date future;

        // Insert one entry, and assert that the office ID is set.
        k = baseDTO().setKvpId(1).setSerial(1).setOpprettetDato(new Date());
        repository.insert(k);
        enhetID = repository.enhetID(AKTOR_ID);
        assertThat(enhetID, equalTo(ENHET_ID));

        // Try fetching data for another actor, and assert that the office ID is unset.
        enhetID = repository.enhetID("***REMOVED***");
        assertNull(enhetID);

        // Finish that entry, and assert that the office ID is unset.
        k = k.setSerial(2).setAvsluttetDato(new Date());
        repository.insert(k);
        enhetID = repository.enhetID(AKTOR_ID);
        assertNull(enhetID);

        // Create an entry where the end date is in the future, and assert that the office ID is set.
        future = new Date();
        future.setTime(future.getTime() + 100000);
        k = baseDTO().setKvpId(2).setSerial(3).setOpprettetDato(new Date()).setAvsluttetDato(future);
        repository.insert(k);
        enhetID = repository.enhetID(AKTOR_ID);
        assertThat(enhetID, equalTo(ENHET_ID));
    }

    /**
     * Test that insert() raises an SQL exception if a row where the start date is NULL is inserted..
     */
    @Test
    public void testInsertNullOpprettetDato() {
        KvpDTO k;

        k = baseDTO().setKvpId(1).setSerial(1);
        assertThrows(DataIntegrityViolationException.class, () -> repository.insert(k));
    }
}
