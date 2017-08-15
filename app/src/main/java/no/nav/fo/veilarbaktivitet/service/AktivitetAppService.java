package no.nav.fo.veilarbaktivitet.service;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.fo.veilarbaktivitet.ws.consumer.AktoerConsumer;
import no.nav.fo.veilarbaktivitet.ws.consumer.ArenaAktivitetConsumer;

import java.util.List;

public abstract class AktivitetAppService {

    private final ArenaAktivitetConsumer arenaAktivitetConsumer;
    private final PepClient pepClient;

    protected final AktivitetService aktivitetService;
    protected final BrukerService brukerService;

    AktivitetAppService(
            ArenaAktivitetConsumer arenaAktivitetConsumer,
            AktivitetService aktivitetService,
            BrukerService brukerService,
            PepClient pepClient
    ) {
        this.arenaAktivitetConsumer = arenaAktivitetConsumer;
        this.aktivitetService = aktivitetService;
        this.brukerService = brukerService;
        this.pepClient = pepClient;
    }

    public List<AktivitetData> hentAktiviteterForIdent(String ident) {
        sjekkTilgangTilFnr(ident);
        return brukerService.getAktorIdForFNR(ident)
                .map(aktivitetService::hentAktiviteterForAktorId)
                .orElseThrow(RuntimeException::new);
    }

    public AktivitetData hentAktivitet(long id) {
        AktivitetData aktivitetData = aktivitetService.hentAktivitet(id);
        sjekkTilgangTilAktorId(aktivitetData.getAktorId());
        return aktivitetData;
    }

    public List<ArenaAktivitetDTO> hentArenaAktiviteter(String ident) {
        sjekkTilgangTilFnr(ident);
        return arenaAktivitetConsumer.hentArenaAktivieter(ident);
    }

    public List<AktivitetData> hentAktivitetVersjoner(long id) {
        sjekkTilgangTilAktivitet(id);
        return aktivitetService.hentAktivitetVersjoner(id);
    }

    public abstract AktivitetData opprettNyAktivtet(String ident, AktivitetData aktivitetData);

    public abstract AktivitetData oppdaterAktivitet(AktivitetData aktivitet);

    public AktivitetData oppdaterStatus(AktivitetData aktivitet) {
        long aktivitetId = aktivitet.getId();
        sjekkTilgangTilAktivitet(aktivitetId);
        return brukerService.getLoggedInnUser()
                .map(userIdent -> {
                    aktivitetService.oppdaterStatus(aktivitet, userIdent);
                    return aktivitetService.hentAktivitet(aktivitetId);
                })
                .orElseThrow(RuntimeException::new);
    }

    public AktivitetData oppdaterEtikett(AktivitetData aktivitet) {
        long aktivitetId = aktivitet.getId();
        sjekkTilgangTilAktivitet(aktivitetId);
        return brukerService.getLoggedInnUser()
                .map(userIdent -> {
                    aktivitetService.oppdaterEtikett(aktivitet, userIdent);
                    return aktivitetService.hentAktivitet(aktivitetId);
                })
                .orElseThrow(RuntimeException::new);
    }

    public void slettAktivitet(long aktivitetId) {
        sjekkTilgangTilAktivitet(aktivitetId);
        aktivitetService.slettAktivitet(aktivitetId);
    }

    protected String sjekkTilgangTilFnr(String ident) {
        return pepClient.sjekkTilgangTilFnr(ident);
    }

    protected void sjekkTilgangTilAktorId(String aktorId) {
        sjekkTilgangTilFnr(brukerService.getFNRForAktorId(aktorId).orElseThrow(IngenTilgang::new));
    }

    protected void sjekkTilgangTilAktivitet(long id) {
        hentAktivitet(id);
    }
}



