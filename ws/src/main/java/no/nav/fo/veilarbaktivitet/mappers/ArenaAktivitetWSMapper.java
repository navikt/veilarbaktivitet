package no.nav.fo.veilarbaktivitet.mappers;

import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.fo.veilarbaktivitet.domain.arena.ArenaAktivitetTypeDTO;
import no.nav.fo.veilarbaktivitet.domain.arena.ArenaStatusDTO;
import no.nav.fo.veilarbaktivitet.domain.arena.MoteplanDTO;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.ArenaAktivitet;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.ArenaAktivitetType;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.ArenaEtikett;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Moeteplan;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.wsStatus;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.xmlCalendar;

public class ArenaAktivitetWSMapper {

    public static ArenaAktivitet mapTilArenaAktivitet(ArenaAktivitetDTO dto) {
        val arena = new ArenaAktivitet();
        arena.setAktivitetId(dto.getId());
        arena.setTittel(dto.getTittel());
        arena.setStatus(wsStatus(dto.getStatus()));
        arena.setType(arenaTypeMap.getKey(dto.getType()));
        arena.setBeskrivelse(dto.getBeskrivelse());
        arena.setFom(xmlCalendar(dto.getFraDato()));
        arena.setTom(xmlCalendar(dto.getTilDato()));
        arena.setOpprettetDato(xmlCalendar(dto.getOpprettetDato()));
        arena.setAvtalt(dto.isAvtalt());
        arena.setEtikett(arenaEtikettMap.getKey(dto.getEtikett()));
        arena.setDeltakelseProsent(dto.getDeltakelseProsent());
        arena.setTiltaksnavn(dto.getTiltaksnavn());
        arena.setTiltakLokaltNavn(dto.getTiltakLokaltNavn());
        arena.setArrangoer(dto.getArrangoer());
        arena.setBedriftsnummer(dto.getBedriftsnummer());
        arena.setAntallDagerPerUke(dto.getAntallDagerPerUke());
        arena.setStatusSistEndret(xmlCalendar(dto.getStatusSistEndret()));

        Optional.ofNullable(dto.getMoeteplanListe())
                .ifPresent(moteListe ->
                        arena.getMoeteplanListe().addAll(moteListe.stream()
                                .map(ArenaAktivitetWSMapper::mapTilMotePlan)
                                .collect(Collectors.toList()))
                );

        return arena;
    }

    private static final BidiMap<ArenaAktivitetType, ArenaAktivitetTypeDTO> arenaTypeMap =
            new DualHashBidiMap<ArenaAktivitetType, ArenaAktivitetTypeDTO>() {{
                put(ArenaAktivitetType.GRUPPEAKTIVITET, ArenaAktivitetTypeDTO.GRUPPEAKTIVITET);
                put(ArenaAktivitetType.TILTAKSAKTIVITET, ArenaAktivitetTypeDTO.TILTAKSAKTIVITET);
                put(ArenaAktivitetType.UTDANNINGSAKTIVITET, ArenaAktivitetTypeDTO.UTDANNINGSAKTIVITET);
            }};

    private static final BidiMap<ArenaEtikett, ArenaStatusDTO> arenaEtikettMap =
            new DualHashBidiMap<ArenaEtikett, ArenaStatusDTO>() {{
                put(ArenaEtikett.AKTUELL, ArenaStatusDTO.AKTUELL);
                put(ArenaEtikett.AVSLAG, ArenaStatusDTO.AVSLAG);
                put(ArenaEtikett.IKKAKTUELL, ArenaStatusDTO.IKKAKTUELL);
                put(ArenaEtikett.IKKEM, ArenaStatusDTO.IKKEM);
                put(ArenaEtikett.INFOMOETE, ArenaStatusDTO.INFOMOETE);
                put(ArenaEtikett.JATAKK, ArenaStatusDTO.JATAKK);
                put(ArenaEtikett.NEITAKK, ArenaStatusDTO.NEITAKK);
                put(ArenaEtikett.TILBUD, ArenaStatusDTO.TILBUD);
                put(ArenaEtikett.VENTELISTE, ArenaStatusDTO.VENTELISTE);
            }};

    private static Moeteplan mapTilMotePlan(MoteplanDTO dto) {
        val mote = new Moeteplan();
        mote.setSluttDato(xmlCalendar(dto.getSluttDato()));
        mote.setStartDato(xmlCalendar(dto.getStartDato()));
        mote.setStartDato(xmlCalendar(dto.getStartDato()));

        return mote;

    }
}
