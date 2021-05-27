package no.nav.veilarbaktivitet.domain;

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
        assertTrue(fnr.isExtern());
        assertTrue(aktorId.isExtern());
        assertFalse(navIdent.isExtern());
    }

    @Test
    public void toInnsenderData() {
        assertEquals(InnsenderData.BRUKER, fnr.toInnsenderData());
        assertEquals(InnsenderData.BRUKER, aktorId.toInnsenderData());
        assertEquals(InnsenderData.NAV, navIdent.toInnsenderData());

    }

}