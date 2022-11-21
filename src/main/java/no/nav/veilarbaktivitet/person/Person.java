package no.nav.veilarbaktivitet.person;

import org.aspectj.weaver.ast.Or;

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

    public static ArenaIdent arenaIdent(String arenaIdent) {
        return new ArenaIdent(arenaIdent);
    }
    public static Organisasjon organisasjon(String orgNr) {
        return new Organisasjon(orgNr);
    }

    public boolean erEkstern() {
        return this instanceof AktorId || this instanceof Fnr;
    }

    public InnsenderData tilBrukerType() {
        return erEkstern() ? InnsenderData.BRUKER : InnsenderData.NAV;
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

    public static class ArenaIdent extends Person {
        private ArenaIdent(String id) { super(id); }
    }

    public static class Organisasjon extends Person {
        private Organisasjon(String orgNr) { super(orgNr); }
    }
}
