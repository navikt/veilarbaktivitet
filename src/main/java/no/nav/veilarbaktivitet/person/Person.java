package no.nav.veilarbaktivitet.person;

import no.nav.veilarbaktivitet.aktivitet.domain.Ident;
import no.nav.veilarbaktivitet.aktivitetskort.dto.IdentType;

import java.util.Objects;

public abstract class Person {
    private final String id;

    private Person(String id) {
        this.id = id;
    }

    public String get() {
        return id;
    }

    public static Fnr fnr(String fnr) {
        return new Fnr(fnr);
    }

    public static AktorId aktorId(String aktorId) {
        return new AktorId(aktorId);
    }

    public static NavIdent navIdent(String navIdent) {
        return new NavIdent(navIdent);
    }

    public boolean erEkstern() {
        return this instanceof AktorId || this instanceof Fnr;
    }

    public Innsender tilInnsenderType() {
        return erEkstern() ? Innsender.BRUKER : Innsender.NAV;
    }

    public Ident tilIdent() {
        if (this instanceof Fnr || this instanceof AktorId) return new Ident(this.get(), IdentType.PERSONBRUKERIDENT);
        return new Ident(this.get(), IdentType.NAVIDENT);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return id.equals(person.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class Fnr extends Person {
        private Fnr(String id) {
            super(id);
        }
    }

    public static class AktorId extends Person {

        private AktorId(String id) {
            super(id);
        }
    }

    public static class NavIdent extends Person {
        private NavIdent(String id) {
            super(id);
        }
    }
}
