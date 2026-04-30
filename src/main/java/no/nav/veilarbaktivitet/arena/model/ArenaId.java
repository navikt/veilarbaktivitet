package no.nav.veilarbaktivitet.arena.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JacksonSerializable;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import lombok.EqualsAndHashCode;

import java.util.List;

/*
 * This class wraps a single string-field to a more type-safe ArenaId
 * Will serialize to a single value, not a json-object with a field named "id"
 * */
@EqualsAndHashCode(callSuper = false)
public class ArenaId extends JacksonSerializable.Base {
    public static final String PREFIX_TILTAK = "TA";
    public static final String PREFIX_GRUPPE = "GA";
    public static final String PREFIX_UTDANNING = "UA";

    public static final String ARENA_PREFIX = "ARENA";
    public static final List<String> validCompletePrefixes = List.of(
            ARENA_PREFIX + PREFIX_TILTAK,
            ARENA_PREFIX + PREFIX_GRUPPE,
            ARENA_PREFIX + PREFIX_UTDANNING
    );
    public static final List<String> validPartialPrefixes = List.of(
            PREFIX_TILTAK,
            PREFIX_GRUPPE,
            PREFIX_UTDANNING
    );
    private final String id;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public ArenaId(String id) {
        super();
        if (validCompletePrefixes.stream().anyMatch(id::startsWith)) {
            this.id = id.trim();
        } else if (validPartialPrefixes.stream().anyMatch(id::startsWith))
        {
            this.id = ARENA_PREFIX + id.trim();
        }
        else {
            throw new  IllegalArgumentException("Argument: %s is not a valid ArenaId".formatted(id));
        }
    }

    public String id() {
        return id;
    }

    @Override
    public void serialize(JsonGenerator jsonGenerator, SerializationContext serializationContext) {
        jsonGenerator.writeString(id);
    }

    @Override
    public void serializeWithType(JsonGenerator jsonGenerator, SerializationContext serializationContext, TypeSerializer typeSerializer) {
        throw new UnsupportedOperationException("Not supported.");
    }
}
