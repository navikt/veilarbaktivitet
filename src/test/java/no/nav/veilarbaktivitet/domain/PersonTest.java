package no.nav.veilarbaktivitet.domain;

import no.nav.veilarbaktivitet.person.InnsenderData;
import no.nav.veilarbaktivitet.person.Person;
import org.junit.Before;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PersonTest {
    private static final String FNR = "10101012350";
    private static final String AKTORID = "10101012350";
    private static final String NAVIDENT = "Z999999";

    Person fnr;
    Person aktorId;
    Person navIdent;

    @Before
    public void setUp() {
        fnr = Person.fnr(FNR);
        aktorId = Person.aktorId(AKTORID);
        navIdent = Person.navIdent(NAVIDENT);
    }

    @Test
    public void isExtern() {
        assertTrue(fnr.erEkstern());
        assertTrue(aktorId.erEkstern());
        assertFalse(navIdent.erEkstern());
    }

    @Test
    public void toInnsenderData() {
        assertEquals(InnsenderData.BRUKER, fnr.tilBrukerType());
        assertEquals(InnsenderData.BRUKER, aktorId.tilBrukerType());
        assertEquals(InnsenderData.NAV, navIdent.tilBrukerType());
    }

}
