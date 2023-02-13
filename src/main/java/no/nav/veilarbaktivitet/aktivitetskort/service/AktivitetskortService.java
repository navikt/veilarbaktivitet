package no.nav.veilarbaktivitet.aktivitetskort.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.Ident;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetsMessageDAO;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortCompareUtil;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortMapper;
import no.nav.veilarbaktivitet.aktivitetskort.dto.bestilling.AktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.dto.bestilling.ArenaAktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.dto.bestilling.EksternAktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.feil.AktivitetsKortFunksjonellException;
import no.nav.veilarbaktivitet.aktivitetskort.feil.ManglerOppfolgingsperiodeFeil;
import no.nav.veilarbaktivitet.aktivitetskort.feil.UlovligEndringFeil;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.util.DateUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;
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

    private final OppfolgingsperiodeService oppfolgingsperiodeService;


    public UpsertActionResult upsertAktivitetskort(AktivitetskortBestilling bestilling) throws AktivitetsKortFunksjonellException {
        var aktivitetskort = bestilling.getAktivitetskort();
        var gammelAktivitetVersjon = aktivitetDAO.hentAktivitetByFunksjonellId(aktivitetskort.getId());

        if (gammelAktivitetVersjon.isPresent()) {
            // Arenaaktiviteter er blitt "ekstern"-aktivitet etter de har blitt opprettet
            var oppdatertAktivitet = oppdaterAktivitet(gammelAktivitetVersjon.get(), bestilling.toAktivitet());
            log.info("Oppdaterte ekstern aktivitetskort {}", oppdatertAktivitet);
            return UpsertActionResult.OPPDATER;
        } else {
            var opprettetAktivitet = opprettAktivitet(bestilling);
            log.info("Opprettet ekstern aktivitetskort {}", opprettetAktivitet);
            return UpsertActionResult.OPPRETT;
        }
    }

    private AktivitetData opprettAktivitet(AktivitetskortBestilling bestilling) throws ManglerOppfolgingsperiodeFeil {
        if (bestilling instanceof ArenaAktivitetskortBestilling arenaAktivitetskortBestilling) {
            return arenaAktivitetskortService.opprettAktivitet(arenaAktivitetskortBestilling);
        } else if (bestilling instanceof EksternAktivitetskortBestilling eksternAktivitetskortBestilling) {
            return opprettEksternAktivitet(eksternAktivitetskortBestilling);
        } else {
            throw new IllegalStateException("Unexpected value: " + bestilling);
        }
    }

    private AktivitetData opprettEksternAktivitet(EksternAktivitetskortBestilling bestilling) throws ManglerOppfolgingsperiodeFeil {
        var endretAv = bestilling.getAktivitetskort().getEndretAv();
        var opprettet = bestilling.getAktivitetskort().getEndretTidspunkt().toLocalDateTime();
        var oppfolgingsperiode = oppfolgingsperiodeService.finnOppfolgingsperiode(bestilling.getAktorId(), opprettet);
        if (oppfolgingsperiode == null) throw new ManglerOppfolgingsperiodeFeil();

        var aktivitetData = AktivitetskortMapper
                .mapTilAktivitetData(bestilling, bestilling.getAktivitetskort().getEndretTidspunkt());

        if (oppfolgingsperiode.sluttDato() != null) {
            aktivitetData.getEksternAktivitetData().setOpprettetSomHistorisk(true);
            aktivitetData.getEksternAktivitetData().setOppfolgingsperiodeSlutt(oppfolgingsperiode.sluttDato().toLocalDateTime());
        }
        return aktivitetService.opprettAktivitet(Person.aktorId(aktivitetData.getAktorId()), aktivitetData, endretAv, opprettet, oppfolgingsperiode.uuid());
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
                new Ident(nyAktivitet.getEndretAv(), nyAktivitet.getEndretAvType()),
                DateUtils.dateToLocalDateTime(nyAktivitet.getEndretDato())
            );
        } else {
            return aktivitet;
        }
    }

    private AktivitetData oppdaterAktivitet(AktivitetData gammelAktivitet, AktivitetData nyAktivitet) throws UlovligEndringFeil {
        if (!Objects.equals(gammelAktivitet.getAktorId(), nyAktivitet.getAktorId())) throw new UlovligEndringFeil("Kan ikke endre bruker på samme aktivitetskort");
        if (gammelAktivitet.getHistoriskDato() != null) throw new UlovligEndringFeil("Kan ikke endre aktiviteter som er historiske (avsluttet oppfølgingsperiode)");
        //TODO vurder avtalt-logikken https://trello.com/c/dFyre4EK
        if (gammelAktivitet.isAvtalt() && !nyAktivitet.isAvtalt()) throw new UlovligEndringFeil("Kan ikke oppdatere fra avtalt til ikke-avtalt");

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
                    new Ident(nyAktivitet.getEndretAv(), nyAktivitet.getEndretAvType()),
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

    public void oppdaterMeldingResultat(UUID messageId, UpsertActionResult upsertActionResult, String reason) {
        aktivitetsMessageDAO.updateActionResult(messageId, upsertActionResult, reason);
    }

}
