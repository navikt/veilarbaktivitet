package no.nav.veilarbaktivitet.arena.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import lombok.EqualsAndHashCode;
import java.io.IOException;

import static no.nav.veilarbaktivitet.arena.VeilarbarenaMapper.ARENA_PREFIX;

@EqualsAndHashCode
public class ArenaId extends JsonSerializable.Base {
    private final String id;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public ArenaId(String id) {
        if (id.startsWith(ARENA_PREFIX)) {
            this.id = id;
        } else {
            this.id = ARENA_PREFIX + id;
        }
    }

    public String id() {
        return id;
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
