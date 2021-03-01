package no.nav.veilarbaktivitet.arena;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.avtaltMedNav.Forhaandsorientering;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static no.nav.veilarbaktivitet.domain.AktivitetStatus.AVBRUTT;
import static no.nav.veilarbaktivitet.domain.AktivitetStatus.FULLFORT;

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

    public boolean harAktiveTiltak(Person.Fnr ident) {
        return consumer.hentArenaAktiviteter(ident)
                .stream()
                .map(ArenaAktivitetDTO::getStatus)
                .anyMatch(status -> status != AVBRUTT && status != FULLFORT);
    }

    public Optional<ArenaAktivitetDTO> hentAktivitet(Person.Fnr ident, String aktivitetId) {
        return hentAktiviteter(ident).stream().filter(arenaAktivitetDTO -> aktivitetId.equals(arenaAktivitetDTO.getId())).findAny();
    }

    ArenaAktivitetDTO lagreForhaandsorientering(ArenaAktivitetDTO arenaAktivitetDTO, Person.AktorId aktorId, Forhaandsorientering forhaandsorientering) {
        dao.insertForhaandsorientering(arenaAktivitetDTO.getId(), aktorId, forhaandsorientering);
        return arenaAktivitetDTO.setForhaandsorientering(forhaandsorientering);
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

    public ArenaAktivitetDTO lagreForhaandsorientering(String arenaaktivitetId, Person.AktorId aktorId, Person.Fnr fnr, Forhaandsorientering forhaandsorientering) throws BadRequestException {
        ArenaAktivitetDTO arenaAktivitetDTO = hentAktivitet(fnr, arenaaktivitetId)
                .orElseThrow(() -> new BadRequestException("Aktiviteten finnes ikke"));

        try {
            dao.insertForhaandsorientering(arenaaktivitetId, aktorId, forhaandsorientering);
            return  arenaAktivitetDTO.setForhaandsorientering(forhaandsorientering);
        } catch (DuplicateKeyException e) {
            throw new BadRequestException("Det er allerede sendt forhaandsorientering på aktiviteten");
        }

    }
}
