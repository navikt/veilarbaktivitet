package no.nav.veilarbaktivitet.arena.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import static no.nav.veilarbaktivitet.arena.VeilarbarenaMapper.ARENA_PREFIX;


public record ArenaId(
        String id
) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public ArenaId(String id) {
        if (id.startsWith(ARENA_PREFIX)) {
            this.id = id;
        } else {
            this.id = ARENA_PREFIX + id;
        }
    }
}
