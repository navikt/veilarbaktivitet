package no.nav.veilarbaktivitet.arena;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ArenaService {
    private final ArenaAktivitetConsumer consumer;
    private final ArenaForhaandsorienteringDAO dao;


    public List<ArenaAktivitetDTO> hentAktiviteter(Person.Fnr ident) {
        List<ArenaAktivitetDTO> aktiviteterFraArena = consumer.hentArenaAktiviteter(ident);
        List<ArenaForhaandsorienteringData> forhaandsorienteringData = dao.hentForhaandsorienteringer(aktiviteterFraArena);

        return aktiviteterFraArena
                .stream()
                .map(mergeMedForhaandsorientering(forhaandsorienteringData))
                .collect(Collectors.toList());
    }

    public Optional<ArenaAktivitetDTO> hentAktivitet(Person.Fnr ident, String aktivitetId) {
        return hentAktiviteter(ident).stream().filter(arenaAktivitetDTO -> arenaAktivitetDTO.getId().equals(aktivitetId)).findAny();
    }

    private Function<ArenaAktivitetDTO, ArenaAktivitetDTO> mergeMedForhaandsorientering(List<ArenaForhaandsorienteringData> forhaandsorienteringData) {
        return arenaAktivitetDTO -> arenaAktivitetDTO.setForhaandsorientering(forhaandsorienteringData
                .stream()
                .filter(arenaForhaandsorienteringData -> arenaForhaandsorienteringData.getArenaktivitetId().equals(arenaAktivitetDTO.getId()))
                .findAny()
                .map(ArenaForhaandsorienteringData::getForhaandsorientering)
                .orElse(null)
        );
    }

}
