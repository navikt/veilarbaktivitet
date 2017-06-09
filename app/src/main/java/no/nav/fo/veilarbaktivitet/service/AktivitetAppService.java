package no.nav.fo.veilarbaktivitet.service;

import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.fo.veilarbaktivitet.ws.consumer.AktoerConsumer;
import no.nav.fo.veilarbaktivitet.ws.consumer.ArenaAktivitetConsumer;

import java.util.List;

public abstract class AktivitetAppService {

    private final AktoerConsumer aktoerConsumer;

    private final ArenaAktivitetConsumer arenaAktivitetConsumer;

    protected final AktivitetService aktivitetService;

    AktivitetAppService(AktoerConsumer aktoerConsumer,
                        ArenaAktivitetConsumer arenaAktivitetConsumer,
                        AktivitetService aktivitetService) {
        this.aktoerConsumer = aktoerConsumer;
        this.arenaAktivitetConsumer = arenaAktivitetConsumer;
        this.aktivitetService = aktivitetService;
    }

    private String hentAktoerIdForIdent(String ident) {
        return aktoerConsumer.hentAktoerIdForIdent(ident)
                .orElseThrow(RuntimeException::new);
    }

    public List<AktivitetData> hentAktiviteterForIdent(String ident) {
        val aktorId = hentAktoerIdForIdent(ident);
        return aktivitetService.hentAktiviteterForAktorId(aktorId);
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

    public AktivitetData opprettNyAktivtet(String ident, AktivitetData aktivitetData) {
        val aktorId = hentAktoerIdForIdent(ident);
        val aktivitetId = aktivitetService.opprettAktivitet(aktorId, aktivitetData);

        return hentAktivitet(aktivitetId);
    }

    public abstract AktivitetData oppdaterAktivitet(AktivitetData aktivitet);

    public AktivitetData oppdaterStatus(AktivitetData aktivitet) {
        aktivitetService.oppdaterStatus(aktivitet);
        return aktivitetService.hentAktivitet(aktivitet.getId());
    }

    public AktivitetData oppdaterEtikett(AktivitetData aktivtet) {
        aktivitetService.oppdaterEtikett(aktivtet);
        return aktivitetService.hentAktivitet(aktivtet.getId());
    }

    public void slettAktivitet(long aktivitetId) {
        aktivitetService.slettAktivitet(aktivitetId);
    }
}



