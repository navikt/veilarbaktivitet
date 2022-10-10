package no.nav.veilarbaktivitet.arena.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

import static no.nav.veilarbaktivitet.arena.VeilarbarenaMapper.ARENA_PREFIX;

class ArenaIdDeserializer extends StdDeserializer<ArenaId> {

    public ArenaIdDeserializer() {
        this(null);
    }

    protected ArenaIdDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public ArenaId deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        return new ArenaId(jsonParser.getText());
    }
}


public record ArenaId(
        String id
) {
    public ArenaId(String id) {
        if (id.startsWith(ARENA_PREFIX)) {
            this.id = id;
        } else {
            this.id = ARENA_PREFIX + id;
        }
    }

    public static ArenaId fromString(String value) {
        return new ArenaId(value);
    }
}
