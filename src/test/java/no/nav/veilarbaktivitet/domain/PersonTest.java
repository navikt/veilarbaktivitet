package no.nav.veilarbaktivitet.domain;

import no.nav.veilarbaktivitet.person.Innsender;
import no.nav.veilarbaktivitet.person.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PersonTest {
    private static final String FNR = "10101012350";
    private static final String AKTORID = "10101012350";
    private static final String NAVIDENT = "Z999999";

    Person fnr;
    Person aktorId;
    Person navIdent;

    @BeforeEach
    void setUp() {
        fnr = Person.fnr(FNR);
        aktorId = Person.aktorId(AKTORID);
        navIdent = Person.navIdent(NAVIDENT);
    }

    @Test
    void isExtern() {
        assertTrue(fnr.erEkstern());
        assertTrue(aktorId.erEkstern());
        assertFalse(navIdent.erEkstern());
    }

    @Test
    void toInnsenderData() {
        assertEquals(Innsender.BRUKER, fnr.tilBrukerType());
        assertEquals(Innsender.BRUKER, aktorId.tilBrukerType());
        assertEquals(Innsender.NAV, navIdent.tilBrukerType());
    }

}
