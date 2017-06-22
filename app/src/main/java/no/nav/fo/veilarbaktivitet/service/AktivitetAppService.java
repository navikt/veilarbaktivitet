package no.nav.fo.veilarbaktivitet.service;

import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.fo.veilarbaktivitet.ws.consumer.ArenaAktivitetConsumer;

import java.util.List;

public abstract class AktivitetAppService {

    private final ArenaAktivitetConsumer arenaAktivitetConsumer;

    protected final AktivitetService aktivitetService;

    protected final BrukerService brukerService;

    AktivitetAppService(ArenaAktivitetConsumer arenaAktivitetConsumer,
                        AktivitetService aktivitetService,
                        BrukerService brukerService) {
        this.arenaAktivitetConsumer = arenaAktivitetConsumer;
        this.aktivitetService = aktivitetService;
        this.brukerService = brukerService;
    }

    public List<AktivitetData> hentAktiviteterForIdent(String ident) {
        return brukerService.getAktorIdForFNR(ident)
                .map(aktivitetService::hentAktiviteterForAktorId)
                .orElseThrow(RuntimeException::new);
    }

    public AktivitetData hentAktivitet(long id) {
        return aktivitetService.hentAktivitet(id);
    }

    public List<ArenaAktivitetDTO> hentArenaAktiviteter(String ident) {
        return arenaAktivitetConsumer.hentArenaAktivieter(ident);
    }

    public List<AktivitetData> hentAktivitetVersjoner(long id) {
        return aktivitetService.hentAktivitetVersjoner(id);
    }

    public abstract AktivitetData opprettNyAktivtet(String ident, AktivitetData aktivitetData);

    public abstract AktivitetData oppdaterAktivitet(AktivitetData aktivitet);

    public AktivitetData oppdaterStatus(AktivitetData aktivitet) {
        return brukerService.getLoggedInnUser()
                .map(userIdent -> {
                    aktivitetService.oppdaterStatus(aktivitet, userIdent);
                    return aktivitetService.hentAktivitet(aktivitet.getId());
                })
                .orElseThrow(RuntimeException::new);
    }

    public AktivitetData oppdaterEtikett(AktivitetData aktivitet) {
        return brukerService.getLoggedInnUser()
                .map(userIdent -> {
                    aktivitetService.oppdaterEtikett(aktivitet, userIdent);
                    return aktivitetService.hentAktivitet(aktivitet.getId());
                })
                .orElseThrow(RuntimeException::new);
    }

    public void slettAktivitet(long aktivitetId) {
        aktivitetService.slettAktivitet(aktivitetId);
    }
}



