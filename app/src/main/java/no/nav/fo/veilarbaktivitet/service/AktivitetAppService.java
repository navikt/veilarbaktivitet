package no.nav.fo.veilarbaktivitet.service;

import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.fo.veilarbaktivitet.ws.consumer.ArenaAktivitetConsumer;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        List<AktivitetData> aktiviteter = brukerService.getAktorIdForFNR(ident)
                .map(aktivitetService::hentAktiviteterForAktorId)
                .orElseThrow(RuntimeException::new);
        return filterKontorsperret(aktiviteter);
    }

    public AktivitetData hentAktivitet(long id) {
        AktivitetData aktivitetData = aktivitetService.hentAktivitet(id);
        sjekkTilgangTilAktorId(aktivitetData.getAktorId());
        assertCanAccessKvpActivity(aktivitetData);
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

    protected AktivitetData internalOppdaterStatus(AktivitetData aktivitetData) {
        return brukerService.getLoggedInnUser()
                .map(userIdent -> {
                    aktivitetService.oppdaterStatus(aktivitetData, userIdent);
                    return aktivitetService.hentAktivitet(aktivitetData.getId());
                })
                .orElseThrow(RuntimeException::new);
    }

    public abstract AktivitetData oppdaterStatus(AktivitetData aktivitet);

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

    /**
     * Take a list of activities, filter out any that can not be accessed due
     * to insufficient kontorsperre privileges, and return the remainder.
     */
    private List<AktivitetData> filterKontorsperret(List<AktivitetData> list) {
        return list.stream().sequential()
                .filter(this::canAccessKvpActivity)
                .collect(Collectors.toList());
    }

    /**
     * Checks the activity for KVP status, and throws an exception if the
     * current user does not have access to the activity.
     */
    private void assertCanAccessKvpActivity(AktivitetData aktivitet) {
        if (!canAccessKvpActivity(aktivitet)) {
            throw new Feil(Feil.Type.INGEN_TILGANG);
        }
    }

    /**
     * Checks the activity for KVP status, and returns true if the current user
     * can access the activity. If the activity is not tagged with KVP, true
     * is always returned.
     */
    private boolean canAccessKvpActivity(AktivitetData aktivitet) {
        return Optional.ofNullable(aktivitet.getKontorsperreEnhetId())
                .map(id -> {
                    try {
                        return pepClient.harTilgangTilEnhet(id);
                    } catch (PepException e) {
                        throw new Feil(Feil.Type.SERVICE_UNAVAILABLE, "Kan ikke kontakte ABAC for utleding av kontorsperre.");
                    }
                })
                .orElse(true);

    }

    protected String sjekkTilgangTilFnr(String ident) {
        return pepClient.sjekkLeseTilgangTilFnr(ident);
    }

    protected void sjekkTilgangTilAktorId(String aktorId) {
        sjekkTilgangTilFnr(brukerService.getFNRForAktorId(aktorId).orElseThrow(IngenTilgang::new));
    }

    protected void sjekkTilgangTilAktivitet(long id) {
        AktivitetData aktivitet = hentAktivitet(id);
        assertCanAccessKvpActivity(aktivitet);
    }
}
