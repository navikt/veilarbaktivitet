package no.nav.veilarbaktivitet.domain;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarbaktivitet.aktivitet.domain.Ident;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.IdentType;
import no.nav.veilarbaktivitet.person.Innsender;
import no.nav.veilarbaktivitet.person.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PersonTest {
    private static final String FNR = "10101012350";
    private static final String AKTORID = "10101012350";
    private static final String NAVIDENT = "Z999999";
    private static final String SYSTEMUSER = "SYSTEM";

    Person fnr;
    Person aktorId;
    Person navIdent;
    Person systemUser;

    @BeforeEach
    void setUp() {
        fnr = Person.fnr(FNR);
        aktorId = Person.aktorId(AKTORID);
        navIdent = Person.navIdent(NAVIDENT);
        systemUser = Person.systemUser();
    }

    @Test
    void isExtern() {
        assertTrue(fnr.erEkstern());
        assertTrue(aktorId.erEkstern());
        assertFalse(navIdent.erEkstern());
        assertFalse(systemUser.erEkstern());
    }

    @Test
    void toInnsenderData() {
        assertEquals(Innsender.BRUKER, fnr.tilInnsenderType());
        assertEquals(Innsender.BRUKER, aktorId.tilInnsenderType());
        assertEquals(Innsender.NAV, navIdent.tilInnsenderType());
        assertEquals(Innsender.SYSTEM, systemUser.tilInnsenderType());
    }

    @Test
    void tilIdent() {
        var fnrIdent = new Ident(fnr.get(), IdentType.PERSONBRUKERIDENT);
        var aktorIdent = new Ident(aktorId.get(), IdentType.PERSONBRUKERIDENT);
        var navIdentIdent = new Ident(navIdent.get(), IdentType.NAVIDENT);
        var systemIdent = new Ident(systemUser.get(), IdentType.SYSTEM);
        assertEquals(fnrIdent, fnr.tilIdent());
        assertEquals(aktorIdent, aktorId.tilIdent());
        assertEquals(navIdentIdent, navIdent.tilIdent());
        assertEquals(systemIdent, systemUser.tilIdent());
    }

    @Test
    void eksternBrukerId() {
        assertEquals(EksternBrukerId.Type.FNR, fnr.eksternBrukerId().type());
        assertEquals(EksternBrukerId.Type.AKTOR_ID, aktorId.eksternBrukerId().type());
        assertThrows(IllegalStateException.class, () -> navIdent.eksternBrukerId());
        assertThrows(IllegalStateException.class, () -> systemUser.eksternBrukerId());
    }

    @Test
    void testOf() {
        no.nav.common.types.identer.Fnr fnr = Fnr.of(FNR);
        no.nav.common.types.identer.AktorId aktorId = AktorId.of(AKTORID);
        no.nav.common.types.identer.NavIdent navIdent = NavIdent.of(NAVIDENT);

        assertTrue(Person.of(fnr) instanceof Person.Fnr);
        assertTrue(Person.of(aktorId) instanceof Person.AktorId);
        assertTrue(Person.of(navIdent) instanceof Person.NavIdent);
    }

    @Test
    void testFnrOtherFnr() {
        assertEquals(Person.fnr(FNR).otherFnr(), Fnr.of(FNR));
    }

}
