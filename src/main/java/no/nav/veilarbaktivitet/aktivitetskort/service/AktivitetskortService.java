package no.nav.veilarbaktivitet.aktivitetskort.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.aktivitet.AktivitetAppService;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitetskort.*;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.AktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.EksternAktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.feil.AktivitetsKortFunksjonellException;
import no.nav.veilarbaktivitet.aktivitetskort.feil.IkkeUnderOppfolgingsFeil;
import no.nav.veilarbaktivitet.aktivitetskort.feil.UlovligEndringFeil;
import no.nav.veilarbaktivitet.oppfolging.siste_periode.IngenGjeldendePeriodeException;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.util.DateUtils;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Stream;


@Service
@RequiredArgsConstructor
@Slf4j
public class AktivitetskortService {

    private final AktivitetService aktivitetService;
    private final AktivitetAppService aktivitetAppService;
    private final AktivitetDAO aktivitetDAO;
    private final AktivitetsMessageDAO aktivitetsMessageDAO;
    private final ArenaAktivitetskortService arenaAktivitetskortService;


    public void upsertAktivitetskort(AktivitetskortBestilling bestilling) throws AktivitetsKortFunksjonellException {
        var aktivitetskort = bestilling.getAktivitetskort();
        var gammelAktivitetVersjon = aktivitetDAO.hentAktivitetByFunksjonellId(aktivitetskort.getId());

        if (gammelAktivitetVersjon.isPresent()) {
            // Arenaaktiviteter er blitt "ekstern"-aktivitet etter de har blitt opprettet
            var oppdatertAktivitet = oppdaterAktivitet(gammelAktivitetVersjon.get(), bestilling.toAktivitet());
            log.info("Oppdaterte ekstern aktivitetskort {}", oppdatertAktivitet);
        } else {
            var opprettetAktivitet = opprettAktivitet(bestilling);
            log.info("Opprettet ekstern aktivitetskort {}", opprettetAktivitet);
        }
    }

    private AktivitetData opprettAktivitet(AktivitetskortBestilling bestilling) throws IkkeUnderOppfolgingsFeil {
        if (bestilling instanceof ArenaAktivitetskortBestilling arenaAktivitetskortBestilling) {
            return arenaAktivitetskortService.opprettAktivitet(arenaAktivitetskortBestilling);
        } else if (bestilling instanceof EksternAktivitetskortBestilling eksternAktivitetskortBestilling) {
            return opprettEksternAktivitet(eksternAktivitetskortBestilling);
        } else {
            throw new IllegalStateException("Unexpected value: " + bestilling);
        }
    }

    private AktivitetData opprettEksternAktivitet(EksternAktivitetskortBestilling bestilling) throws IkkeUnderOppfolgingsFeil {
        Person endretAvIdent = bestilling.getAktivitetskort().getEndretAv().toPerson();
        var opprettet = bestilling.getAktivitetskort().getEndretTidspunkt();
        var aktivitetData = AktivitetskortMapper
                .mapTilAktivitetData(bestilling, bestilling.getAktivitetskort().getEndretTidspunkt());
        try {
            return aktivitetService.opprettAktivitet(Person.aktorId(aktivitetData.getAktorId()), aktivitetData, endretAvIdent, opprettet);
        } catch (IngenGjeldendePeriodeException e) {
            throw new IkkeUnderOppfolgingsFeil(e);
        }
    }

    private AktivitetData oppdaterDetaljer(AktivitetData aktivitet, AktivitetData nyAktivitet) {
        if (AktivitetskortCompareUtil.erFaktiskOppdatert(nyAktivitet, aktivitet)) {
            return aktivitetService.oppdaterAktivitet(aktivitet, nyAktivitet, Person.navIdent(nyAktivitet.getEndretAv()), DateUtils.dateToLocalDateTime(nyAktivitet.getEndretDato()));
        }
        return aktivitet;
    }
    public AktivitetData oppdaterStatus(AktivitetData aktivitet, AktivitetData nyAktivitet) {
        if (aktivitet.getStatus() != nyAktivitet.getStatus()) {
            return aktivitetService.oppdaterStatus(
                aktivitet,
                nyAktivitet, // TODO: Populer avbrutt-tekstfelt
                Person.arenaIdent(nyAktivitet.getEndretAv()), // TODO håndtere rikere identType
                DateUtils.dateToLocalDateTime(nyAktivitet.getEndretDato())
            );
        } else {
            return aktivitet;
        }
    }

    private AktivitetData oppdaterAktivitet(AktivitetData gammelAktivitet, AktivitetData nyAktivitet) throws UlovligEndringFeil {
        if (!gammelAktivitet.endringTillatt()) throw new UlovligEndringFeil();
        return Stream.of(gammelAktivitet)
                .map( aktivitet -> settAvtaltHvisAvtalt( aktivitet, nyAktivitet))
                .map( aktivitet -> oppdaterDetaljer(aktivitet, nyAktivitet))
                .map( aktivitet -> oppdaterStatus(aktivitet, nyAktivitet))
                .findFirst().orElse(null);
    }

    private AktivitetData settAvtaltHvisAvtalt(AktivitetData originalAktivitet, AktivitetData nyAktivitet) {
        if (nyAktivitet.isAvtalt() && !originalAktivitet.isAvtalt()) {
            return aktivitetService.settAvtalt(
                    originalAktivitet,
                    Person.arenaIdent(nyAktivitet.getEndretAv()), // TODO fix identtype på aktivitet
                    DateUtils.dateToLocalDateTime(nyAktivitet.getEndretDato()));
        } else {
            return originalAktivitet;
        }
    }

    public boolean harSettMelding(UUID messageId) {
        return aktivitetsMessageDAO.exist(messageId);
    }

    public void lagreMeldingsId(UUID messageId, UUID funksjonellId) {
        aktivitetsMessageDAO.insert(messageId, funksjonellId);
    }

}
