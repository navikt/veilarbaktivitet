package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDAO;
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


    private final PersonService personService;

    public void upsertAktivitetskort(TiltaksaktivitetDTO tiltaksaktivitet) throws UlovligEndringFeil, UgyldigIdentFeil {
        Optional<AktivitetData> maybeAktivitet = aktivitetDAO.hentAktivitetByFunksjonellId(tiltaksaktivitet.id);


        Person.AktorId aktorIdForPersonBruker = hentAktorId(Person.fnr(tiltaksaktivitet.personIdent));

        var aktivitetData = maybeAktivitet
                .map(gammelaktivitet -> AktivitetskortMapper.mapTilAktivitetData(tiltaksaktivitet, dateToLocalDateTime(gammelaktivitet.getOpprettetDato()), tiltaksaktivitet.endretDato, aktorIdForPersonBruker.get()))
                .orElse(AktivitetskortMapper.mapTilAktivitetData(tiltaksaktivitet, tiltaksaktivitet.endretDato, tiltaksaktivitet.endretDato, aktorIdForPersonBruker.get()));

        Person.NavIdent endretAvIdent = Person.navIdent(aktivitetData.getEndretAv());

        if (maybeAktivitet.isPresent()) {
            oppdaterTiltaksAktivitet(maybeAktivitet.get(), aktivitetData);
        } else {
            AktivitetData opprettetAktivitet = opprettTiltaksAktivitet(aktivitetData, endretAvIdent, tiltaksaktivitet.endretDato);
            Optional.ofNullable(forhaandsorienteringDAO.getFhoForArenaAktivitet(new ArenaId(tiltaksaktivitet.getEksternReferanseId())))
                    .ifPresent(fho -> {
                        int updated = forhaandsorienteringDAO.leggTilTekniskId(fho.getId(), opprettetAktivitet.getId());

                        if (updated == 1) {
                            aktivitetService.oppdaterAktivitet(opprettetAktivitet, opprettetAktivitet.withForhaandsorientering(fho), Person.navIdent(fho.getOpprettetAv()));
                        }

                        log.debug("La til teknisk id på FHO med id={}, tekniskId={}", fho.getId(), opprettetAktivitet.getId());
                    });
        }
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
                Person.navIdent(nyAktivitet.getEndretAv()),
                DateUtils.dateToLocalDateTime(nyAktivitet.getEndretDato())
            );
        } else {
            return aktivitet;
        }
    }

    private void oppdaterTiltaksAktivitet(AktivitetData gammelAktivitet, AktivitetData nyAktivitet) throws UlovligEndringFeil {
        if (!gammelAktivitet.endringTillatt()) throw new UlovligEndringFeil();
        Stream.of(gammelAktivitet)
            .map((aktivitet) -> oppdaterDetaljer(aktivitet, nyAktivitet))
            .map((aktivitet) -> oppdaterStatus(aktivitet, nyAktivitet))
            .findFirst();
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
