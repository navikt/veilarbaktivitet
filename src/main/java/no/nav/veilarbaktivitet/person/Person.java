package no.nav.veilarbaktivitet.person;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.Id;
import no.nav.veilarbaktivitet.aktivitet.domain.Ident;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.IdentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * Applikasjonsintern representasjon av en bruker. Kan også være en systembruker, som ikke er en person.
 * Merk JsonSerializable.Base for å støtte deserialisering av en person til en simpel json streng (brukerid).
 * Deserialisering av Person skjer kun i ett tilfelle, og det er under deserialisering av AktivitetData under skriving til topic.ut.aktivitetdata.rawjson.
 * Denne topicen er foreløpig ikke i bruk av andre funksjoner, og skal den tas i bruk,
 *  TODO er det nok fornuftig å mappe AktivitetData om til en dto før serialisering, og fjerne jackson-annotasjonene på denne klassen.
 */
public abstract class Person extends JsonSerializable.Base {
    private final Logger secureLogs = LoggerFactory.getLogger("SecureLog");

    private final String id;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    Person(String id) {
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

    public static SystemUser systemUser() {
        return new SystemUser("SYSTEM");
    }

    public boolean erEkstern() {
        return this instanceof AktorId || this instanceof Fnr;
    }

    private void logWrongTypeToSecureLogs() {
        secureLogs.warn("Person id:{}, type:{}   må være en av Fnr, AktorId, NavIdent eller SystemUser", this.id, this.getClass().getSimpleName());
    }

    public Innsender tilInnsenderType() {
        if (this instanceof Fnr || this instanceof AktorId) return Innsender.BRUKER;
        if (this instanceof NavIdent) return Innsender.NAV;
        if (this instanceof SystemUser) return Innsender.SYSTEM;
        logWrongTypeToSecureLogs();
        throw new IllegalArgumentException("Ukjent persontype %s".formatted(this.getClass().getSimpleName()));
    }

    public Ident tilIdent() {
        if (this instanceof Fnr || this instanceof AktorId) return new Ident(this.get(), IdentType.PERSONBRUKERIDENT);
        if (this instanceof NavIdent) return new Ident(this.get(), IdentType.NAVIDENT);
        if (this instanceof SystemUser) return new Ident(this.get(), IdentType.SYSTEM);
        logWrongTypeToSecureLogs();
        throw new IllegalArgumentException("Ukjent persontype %s".formatted(this.getClass().getSimpleName()));
    }

    public EksternBrukerId eksternBrukerId(){
        if (this instanceof Fnr) return no.nav.common.types.identer.Fnr.of(this.get());
        if (this instanceof AktorId) return no.nav.common.types.identer.AktorId.of(this.get());
        logWrongTypeToSecureLogs();
        throw new IllegalStateException("Bare fnr eller aktorId kan brukes som eksternId, fikk %s".formatted(this.getClass().getSimpleName())
        );
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

        public AktorId(String id) {
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

    public static class SystemUser extends Person {
        private SystemUser(String id) {
            super(id);
        }
    }

    public static Person of(Id id) {
        return switch (id) {
            case no.nav.common.types.identer.Fnr fnr -> fnr(id.get());
            case no.nav.common.types.identer.AktorId aktorId -> aktorId(id.get());
            case no.nav.common.types.identer.NavIdent navIdent -> navIdent(id.get());
            case null, default -> throw new IllegalStateException("Person må være enten fnr, aktørId eller navIdent");
        };
    }

    @Override
    public void serialize(JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(id);
    }
    @Override
    public void serializeWithType(JsonGenerator jsonGenerator, SerializerProvider serializerProvider, TypeSerializer typeSerializer) throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }
}
