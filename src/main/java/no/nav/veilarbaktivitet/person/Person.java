package no.nav.veilarbaktivitet.person;

import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.Id;
import no.nav.veilarbaktivitet.aktivitet.domain.Ident;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.IdentType;

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

    public EksternBrukerId eksternBrukerId(){
        if (this instanceof Fnr) return no.nav.common.types.identer.Fnr.of(this.get());
        if (this instanceof AktorId) return no.nav.common.types.identer.AktorId.of(this.get());
        throw new IllegalStateException("Bare fnr eller aktorId kan brukes som eksternId");
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
        public no.nav.common.types.identer.Fnr otherFnr() { return no.nav.common.types.identer.Fnr.of(this.get()); }
    }

    public static class AktorId extends Person {

        private AktorId(String id) {
            super(id);
        }
        public no.nav.common.types.identer.AktorId otherAktorId() { return no.nav.common.types.identer.AktorId.of(this.get()); }
    }

    public static class NavIdent extends Person {
        private NavIdent(String id) {
            super(id);
        }
        public no.nav.common.types.identer.NavIdent otherNavIdent() { return no.nav.common.types.identer.NavIdent.of(this.get()); }
    }

    public static Person of(Id id) {
        if (id instanceof no.nav.common.types.identer.Fnr) {
            return fnr(id.get());
        } else if (id instanceof no.nav.common.types.identer.AktorId) {
            return aktorId(id.get());
        } else if (id instanceof no.nav.common.types.identer.NavIdent) {
            return navIdent(id.get());
        } else {
            throw new IllegalStateException("Person må være enten fnr, aktørId eller navIdent");
        }
    }
}
