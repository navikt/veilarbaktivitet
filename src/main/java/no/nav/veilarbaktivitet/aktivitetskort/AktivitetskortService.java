package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDAO;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukerNotifikasjonDAO;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import no.nav.veilarbaktivitet.person.IkkeFunnetPersonException;
import no.nav.veilarbaktivitet.person.UgyldigIdentException;
import no.nav.veilarbaktivitet.util.DateUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static no.nav.veilarbaktivitet.util.DateUtils.dateToLocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AktivitetskortService {

    private final AktivitetService aktivitetService;
    private final AktivitetDAO aktivitetDAO;
    private final AktivitetsMessageDAO aktivitetsMessageDAO;
    private final ForhaandsorienteringDAO forhaandsorienteringDAO;
    private final BrukerNotifikasjonDAO brukerNotifikasjonDAO;


    private final PersonService personService;

    public void upsertAktivitetskort(TiltaksaktivitetDTO tiltaksaktivitet, boolean erArenaAktivitet) throws UlovligEndringFeil, UgyldigIdentFeil {
        Optional<AktivitetData> maybeAktivitet = aktivitetDAO.hentAktivitetByFunksjonellId(tiltaksaktivitet.id);
        Person.AktorId aktorIdForPersonBruker = hentAktorId(Person.fnr(tiltaksaktivitet.personIdent));

        Person endretAvIdent = toPerson(tiltaksaktivitet.getEndretAv());
        var aktivitetData = maybeAktivitet
                .map(gammelaktivitet -> AktivitetskortMapper.mapTilAktivitetData(tiltaksaktivitet, dateToLocalDateTime(gammelaktivitet.getOpprettetDato()), tiltaksaktivitet.endretDato, aktorIdForPersonBruker.get()))
                .orElse(AktivitetskortMapper.mapTilAktivitetData(tiltaksaktivitet, tiltaksaktivitet.endretDato, tiltaksaktivitet.endretDato, aktorIdForPersonBruker.get()));

        if (maybeAktivitet.isPresent()) {
            var oppdatertAktivitet = oppdaterTiltaksAktivitet(maybeAktivitet.get(), aktivitetData);
            log.info("Oppdaterte eksternt aktivitetskort {}", oppdatertAktivitet);
        } else {
            var opprettetAktivitet = opprettTiltaksAktivitet(aktivitetData, endretAvIdent, tiltaksaktivitet.endretDato);
            log.info("Opprettet eksternt aktivitetskort {}", opprettetAktivitet);
            if (erArenaAktivitet) {
                arenaspesifikkMigrering(tiltaksaktivitet, opprettetAktivitet);
            }
        }
    }

    private void arenaspesifikkMigrering(TiltaksaktivitetDTO tiltaksaktivitet, AktivitetData opprettetAktivitet) {
        Optional.ofNullable(forhaandsorienteringDAO.getFhoForArenaAktivitet(new ArenaId(tiltaksaktivitet.getEksternReferanseId())))
                .ifPresent(fho -> {
                    int updated = forhaandsorienteringDAO.leggTilTekniskId(fho.getId(), opprettetAktivitet.getId());
                    if (updated == 0) return;
                    aktivitetService.oppdaterAktivitet(
                            opprettetAktivitet,
                            opprettetAktivitet.withFhoId(fho.getId()),
                            Person.navIdent(fho.getOpprettetAv()),
                            DateUtils.dateToLocalDateTime(fho.getOpprettetDato())
                    );
                    log.debug("La til teknisk id på FHO med id={}, tekniskId={}", fho.getId(), opprettetAktivitet.getId());
                });

        // oppdater alle brukernotifikasjoner med aktivitet arena-ider
        brukerNotifikasjonDAO.updateAktivitetIdForArenaBrukernotifikasjon(opprettetAktivitet.getId(), opprettetAktivitet.getVersjon(), new ArenaId(tiltaksaktivitet.eksternReferanseId));
    }

    private Person toPerson(IdentDTO ident) {
        return switch (ident.identType()) {
            case NAVIDENT -> Person.navIdent(ident.ident());
            case PERSONBRUKERIDENT -> Person.fnr(ident.ident());
            case ARENAIDENT -> Person.arenaIdent(ident.ident());
        };
    }

    private Person.AktorId hentAktorId(Person.Fnr fnr) throws UgyldigIdentFeil {
          try {
              return personService.getAktorIdForPersonBruker(fnr).orElseThrow(() -> new UgyldigIdentFeil("Ugyldig identtype for " + fnr.get(), null));
          } catch (UgyldigIdentException e) {
              throw new UgyldigIdentFeil("Ugyldig ident for fnr :" + fnr.get(), e);
          } catch (IkkeFunnetPersonException e) {
              throw new UgyldigIdentFeil("AktørId ikke funnet for fnr :" + fnr.get(), e);
          }
    }

    private AktivitetData oppdaterDetaljer(AktivitetData aktivitet, AktivitetData nyAktivitet) {
        if (AktivitetskortCompareUtil.erFaktiskOppdatert(nyAktivitet, aktivitet)) {
            return aktivitetService.oppdaterAktivitet(aktivitet, nyAktivitet, Person.navIdent(nyAktivitet.getEndretAv()), DateUtils.dateToLocalDateTime(nyAktivitet.getEndretDato()));
        }
        return aktivitet;
    }
    AktivitetData oppdaterStatus(AktivitetData aktivitet, AktivitetData nyAktivitet) {
        if (aktivitet.getStatus() != nyAktivitet.getStatus()) {
            return aktivitetService.oppdaterStatus(
                aktivitet,
                nyAktivitet, // TODO: Populer avbrutt-tekstfelt
                Person.arenaIdent(nyAktivitet.getEndretAv()),
                DateUtils.dateToLocalDateTime(nyAktivitet.getEndretDato())
            );
        } else {
            return aktivitet;
        }
    }

    private AktivitetData oppdaterTiltaksAktivitet(AktivitetData gammelAktivitet, AktivitetData nyAktivitet) throws UlovligEndringFeil {
        if (!gammelAktivitet.endringTillatt()) throw new UlovligEndringFeil();
        return Stream.of(gammelAktivitet)
            .map( aktivitet -> oppdaterDetaljer(aktivitet, nyAktivitet))
            .map( aktivitet -> oppdaterStatus(aktivitet, nyAktivitet))
            .findFirst().orElse(null);
    }

    private AktivitetData opprettTiltaksAktivitet(AktivitetData aktivitetData, Person endretAvIdent, LocalDateTime opprettet) {
        return aktivitetService.opprettAktivitet(Person.aktorId(aktivitetData.getAktorId()), aktivitetData, endretAvIdent, opprettet);
    }

    public boolean harSettMelding(UUID messageId) {
        return aktivitetsMessageDAO.exist(messageId);
    }

    public void lagreMeldingsId(UUID messageId, UUID funksjonellId) {
        aktivitetsMessageDAO.insert(messageId, funksjonellId);
    }

}
