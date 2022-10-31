package no.nav.veilarbaktivitet.aktivitetskort.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitetskort.*;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.AktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.EksternAktivitetskortBestilling;
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


@Service
@RequiredArgsConstructor
@Slf4j
public class AktivitetskortService {

    private final AktivitetService aktivitetService;
    private final AktivitetDAO aktivitetDAO;
    private final AktivitetsMessageDAO aktivitetsMessageDAO;
    private final ArenaAktivitetskortService arenaAktivitetskortService;


    private final PersonService personService;

    public void upsertAktivitetskort(EksternAktivitetskortBestilling bestilling) throws UlovligEndringFeil, UgyldigIdentFeil {
        var aktivitetskort = bestilling.getAktivitetskort();
        var enrichedBestilling = bestilling.withAktorId(hentAktorId(Person.fnr(aktivitetskort.getPersonIdent())));
        Optional<AktivitetData> maybeAktivitet = aktivitetDAO.hentAktivitetByFunksjonellId(aktivitetskort.getId());

        if (maybeAktivitet.isPresent()) {
            var aktivitetData = AktivitetskortMapper.mapTilAktivitetData(
                    enrichedBestilling,
                    null);
            var oppdatertAktivitet = oppdaterEksternAktivitet(maybeAktivitet.get(), aktivitetData);
            log.info("Oppdaterte ekstern aktivitetskort {}", oppdatertAktivitet);
        } else {
            var opprettetAktivitet = opprettAktivitet(enrichedBestilling);
            log.info("Opprettet ekstern aktivitetskort {}", opprettetAktivitet);
            /*
            var opprettetAktivitet = opprettEksternAktivitet(aktivitetData, endretAvIdent, aktivitetskort.endretTidspunkt);
            if (erArenaAktivitet) {
                arenaspesifikkMigrering(aktivitetskort, opprettetAktivitet, meldingContext.eksternReferanseId());
            }*/
        }
    }

    private AktivitetData opprettAktivitet(AktivitetskortBestilling bestilling) {
        return switch (bestilling) {
            case ArenaAktivitetskortBestilling a -> opprettArenaAktivitet(a);
            case EksternAktivitetskortBestilling a -> opprettEksternAktivitet(a);
            default -> throw new IllegalStateException("Unexpected value: " + bestilling);
        };
    }
    private AktivitetData opprettArenaAktivitet(ArenaAktivitetskortBestilling bestilling) {
        return arenaAktivitetskortService.opprettAktivitet(bestilling);
    }
    private AktivitetData opprettEksternAktivitet(EksternAktivitetskortBestilling bestilling) {
        Person endretAvIdent = bestilling.getAktivitetskort().getEndretAv().toPerson();
        var aktivitetData = AktivitetskortMapper
                .mapTilAktivitetData(bestilling, bestilling.getAktivitetskort().getEndretTidspunkt());
        return opprettEksternAktivitet(aktivitetData, endretAvIdent, bestilling.getAktivitetskort().getEndretTidspunkt());
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

    private AktivitetData oppdaterEksternAktivitet(AktivitetData gammelAktivitet, AktivitetData nyAktivitet) throws UlovligEndringFeil {
        if (!gammelAktivitet.endringTillatt()) throw new UlovligEndringFeil();
        return Stream.of(gammelAktivitet)
            .map( aktivitet -> oppdaterDetaljer(aktivitet, nyAktivitet))
            .map( aktivitet -> oppdaterStatus(aktivitet, nyAktivitet))
            .findFirst().orElse(null);
    }

    private AktivitetData opprettEksternAktivitet(AktivitetData aktivitetData, Person endretAvIdent, LocalDateTime opprettet) {
        return aktivitetService.opprettAktivitet(Person.aktorId(aktivitetData.getAktorId()), aktivitetData, endretAvIdent, opprettet);
    }

    public boolean harSettMelding(UUID messageId) {
        return aktivitetsMessageDAO.exist(messageId);
    }

    public void lagreMeldingsId(UUID messageId, UUID funksjonellId) {
        aktivitetsMessageDAO.insert(messageId, funksjonellId);
    }

}
