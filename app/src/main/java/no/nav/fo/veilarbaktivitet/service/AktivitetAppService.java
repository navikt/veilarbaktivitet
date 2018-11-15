package no.nav.fo.veilarbaktivitet.service;

import lombok.val;
import no.nav.apiapp.feil.*;
import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarbaktivitet.domain.*;
import no.nav.fo.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.fo.veilarbaktivitet.util.FunksjonelleMetrikker;
import no.nav.fo.veilarbaktivitet.ws.consumer.ArenaAktivitetConsumer;
import no.nav.metrics.aspects.Timed;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.apiapp.util.ObjectUtils.notEqual;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.AVBRUTT;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.FULLFORT;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.*;
import static no.nav.fo.veilarbaktivitet.service.BrukerService.erEksternBruker;
import static no.nav.fo.veilarbaktivitet.service.BrukerService.erInternBruker;

@Component
public class AktivitetAppService {

    private final ArenaAktivitetConsumer arenaAktivitetConsumer;
    private final PepClient pepClient;
    private final AktivitetService aktivitetService;
    private final BrukerService brukerService;

    @Inject
    AktivitetAppService(ArenaAktivitetConsumer arenaAktivitetConsumer,
                        PepClient pepClient,
                        AktivitetService aktivitetService,
                        BrukerService brukerService) {
        this.arenaAktivitetConsumer = arenaAktivitetConsumer;
        this.pepClient = pepClient;
        this.aktivitetService = aktivitetService;
        this.brukerService = brukerService;
    }


    private static final Set<AktivitetTypeData> TYPER_SOM_KAN_ENDRES_EKSTERNT = new HashSet<>(Arrays.asList(
            EGENAKTIVITET,
            JOBBSOEKING,
            SOKEAVTALE,
            IJOBB,
            BEHANDLING
    ));

    private static final Set<AktivitetTypeData> TYPER_SOM_KAN_OPPRETTES_EKSTERNT = new HashSet<>(Arrays.asList(
            EGENAKTIVITET,
            JOBBSOEKING,
            IJOBB
    ));


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

    public List<AktivitetData> hentAktiviteterForIdent(Person ident) {
        sjekkTilgangTilPerson(ident);
        List<AktivitetData> aktiviteter = brukerService.getAktorIdForPerson(ident)
                .map(aktivitetService::hentAktiviteterForAktorId)
                .orElseThrow(RuntimeException::new);
        return filterKontorsperret(aktiviteter);
    }

    public AktivitetData hentAktivitet(long id) {
        AktivitetData aktivitetData = aktivitetService.hentAktivitet(id);
        settLestAvBrukerHvisUlest(aktivitetData);
        sjekkTilgangTilPerson(Person.aktorId(aktivitetData.getAktorId()));
        assertCanAccessKvpActivity(aktivitetData);
        return aktivitetData;
    }

    public List<ArenaAktivitetDTO> hentArenaAktiviteter(Person.Fnr ident) {
        sjekkTilgangTilPerson(ident);
        return arenaAktivitetConsumer.hentArenaAktiviteter(ident);
    }

    public List<AktivitetData> hentAktivitetVersjoner(long id) {
        hentAktivitet(id); // innebærer tilgangskontroll;
        return aktivitetService.hentAktivitetVersjoner(id)
                .stream()
                .filter(AktivitetAppService::erEksternBrukerOgEndringenSkalVereSynnelig)
                .collect(Collectors.toList());
    }

    public void settLestAvBrukerHvisUlest(AktivitetData aktivitetData) {
        if (erEksternBruker() && aktivitetData.getLestAvBrukerForsteGang() == null) {
            AktivitetData hentetAktivitet = aktivitetService.settLestAvBrukerTidspunkt(aktivitetData.getId());
            FunksjonelleMetrikker.reportAktivitetLestAvBrukerForsteGang(hentetAktivitet);
        }
    }

    private static boolean erEksternBrukerOgEndringenSkalVereSynnelig(AktivitetData aktivitetData) {
        return !erEksternBruker() || erSynligForEksterne(aktivitetData);
    }

    private static boolean erSynligForEksterne(AktivitetData aktivitetData) {
        return !(kanHaInterneForandringer(aktivitetData) && erReferatetEndretForDetErPublisert(aktivitetData));
    }

    private static boolean kanHaInterneForandringer(AktivitetData aktivitetData) {
        return aktivitetData.getAktivitetType() == AktivitetTypeData.MOTE ||
                aktivitetData.getAktivitetType() == AktivitetTypeData.SAMTALEREFERAT;
    }

    private static boolean erReferatetEndretForDetErPublisert(AktivitetData aktivitetData) {
        boolean referatEndret = AktivitetTransaksjonsType.REFERAT_ENDRET.equals(aktivitetData.getTransaksjonsType()) ||
                AktivitetTransaksjonsType.REFERAT_OPPRETTET.equals(aktivitetData.getTransaksjonsType());
        return !aktivitetData.getMoteData().isReferatPublisert() && referatEndret;
    }

    public AktivitetData opprettNyAktivitet(Person ident, AktivitetData aktivitetData) {
        sjekkTilgangTilPerson(ident);

        if (erEksternBruker() && !TYPER_SOM_KAN_OPPRETTES_EKSTERNT.contains(aktivitetData.getAktivitetType())) {
            throw new UgyldigRequest();
        }

        Person.AktorId aktorId = brukerService.getAktorIdForPerson(ident).orElseThrow(RuntimeException::new);
        Person loggedInUser = erEksternBruker() ?
                aktorId :
                brukerService.getLoggedInnUser().orElseThrow(RuntimeException::new);

        long id = aktivitetService.opprettAktivitet(aktorId, aktivitetData, loggedInUser);
        return this.hentAktivitet(id); // this is done because of KVP
    }

    @Transactional
    public AktivitetData oppdaterAktivitet(AktivitetData aktivitet) {
        AktivitetData original = hentAktivitet(aktivitet.getId()); // innebærer tilgangskontroll
        kanEndreAktivitetGuard(original, aktivitet);

        Person loggedInnUser = brukerService.getLoggedInnUser().orElseThrow(RuntimeException::new);

        if (erInternBruker()) {
            if (original.isAvtalt()) {
                if (original.getAktivitetType() == MOTE) {
                    aktivitetService.oppdaterMoteTidOgSted(original, aktivitet, loggedInnUser);
                } else {
                    aktivitetService.oppdaterAktivitetFrist(original, aktivitet, loggedInnUser);
                }
            } else {
                aktivitetService.oppdaterAktivitet(original, aktivitet, loggedInnUser);
            }

            return aktivitetService.hentAktivitet(aktivitet.getId());

        } else if (erEksternBruker()) {
            if (original.isAvtalt() || !TYPER_SOM_KAN_ENDRES_EKSTERNT.contains(original.getAktivitetType())) {
                throw new UgyldigRequest();
            }

            aktivitetService.oppdaterAktivitet(original, aktivitet, loggedInnUser);
            return aktivitetService.hentAktivitet(aktivitet.getId());
        }

        // not a valid user
        throw new IngenTilgang();
    }

    private void kanEndreAktivitetGuard(AktivitetData orginalAktivitet, AktivitetData aktivitet) {
        if (notEqual(orginalAktivitet.getVersjon(), aktivitet.getVersjon())) {
            throw new VersjonsKonflikt();
        } else if (skalIkkeKunneEndreAktivitet(orginalAktivitet)) {
            throw new IllegalArgumentException(
                    String.format("Kan ikke endre aktivitet [%s] i en ferdig status",
                            orginalAktivitet.getId())
            );
        }
    }

    private Boolean skalIkkeKunneEndreAktivitet(AktivitetData aktivitetData) {
        AktivitetStatus status = aktivitetData.getStatus();
        return AVBRUTT.equals(status) || FULLFORT.equals(status) || aktivitetData.getHistoriskDato() != null;
    }

    @Transactional
    public AktivitetData oppdaterStatus(AktivitetData aktivitet) {
        val originalAktivitet = hentAktivitet(aktivitet.getId()); // innebærer tilgangskontroll
        kanEndreAktivitetGuard(originalAktivitet, aktivitet);

        Person endretAv = brukerService
                .getLoggedInnUser()
                .orElseThrow(() -> new Feil(FeilType.SERVICE_UNAVAILABLE, "Aktor tjenesten er nede"));

        if (erInternBruker()) {
            aktivitetService.oppdaterStatus(originalAktivitet, aktivitet, endretAv);
            val newAktivitet = aktivitetService.hentAktivitet(originalAktivitet.getId());
            FunksjonelleMetrikker.oppdatertStatusAvNAV(newAktivitet);
            return newAktivitet;
        } else if (erEksternBruker()) {
            if (TYPER_SOM_KAN_ENDRES_EKSTERNT.contains(originalAktivitet.getAktivitetType())) {
                aktivitetService.oppdaterStatus(originalAktivitet, aktivitet, endretAv);
                val newAktivitet = aktivitetService.hentAktivitet(originalAktivitet.getId());
                FunksjonelleMetrikker.oppdatertStatusAvBruker(newAktivitet);
                return newAktivitet;
            } else {
                throw new UgyldigRequest();
            }
        }

        // not a valid user
        throw new IngenTilgang();
    }

    @Transactional
    public AktivitetData oppdaterEtikett(AktivitetData aktivitet) {
        val originalAktivitet = hentAktivitet(aktivitet.getId()); // innebærer tilgangskontroll
        kanEndreAktivitetGuard(originalAktivitet, aktivitet);
        return brukerService.getLoggedInnUser()
                .map(userIdent -> {
                    aktivitetService.oppdaterEtikett(originalAktivitet, aktivitet, userIdent);
                    return aktivitetService.hentAktivitet(aktivitet.getId());
                })
                .orElseThrow(RuntimeException::new);
    }

    public void slettAktivitet(long aktivitetId) {
        hentAktivitet(aktivitetId); // innebærer tilgangskontroll

        if (erEksternBruker()) {
            aktivitetService.slettAktivitet(aktivitetId);
        } else {
            throw new IngenTilgang();
        }
    }

    @Transactional
    public AktivitetData oppdaterReferat(AktivitetData aktivitet) {
        if (erEksternBruker()) {
            throw new IngenTilgang();
        }

        val originalAktivitet = hentAktivitet(aktivitet.getId());
        kanEndreAktivitetGuard(originalAktivitet, aktivitet);

        aktivitetService.oppdaterReferat(
                originalAktivitet,
                aktivitet,
                brukerService.getLoggedInnUser().orElseThrow(IngenTilgang::new)
        );

        return hentAktivitet(aktivitet.getId());
    }

    /**
     * Take a list of activities, filter out any that can not be accessed due
     * to insufficient kontorsperre privileges, and return the remainder.
     */
    @Timed(name = "kontorsperre.filter.aktivitet")
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
            FunksjonelleMetrikker.reportIngenTilgangGrunnetKontorsperre();
            throw new Feil(FeilType.INGEN_TILGANG);
        }
    }

    /**
     * Checks the activity for KVP status, and returns true if the current user
     * can access the activity. If the activity is not tagged with KVP, true
     * is always returned.
     * <p>
     * This function reports real usage through the metric system.
     */
    private boolean canAccessKvpActivity(AktivitetData aktivitet) {
        boolean hasAccess = Optional.ofNullable(aktivitet.getKontorsperreEnhetId())
                .map(id -> {
                    try {
                        return pepClient.harTilgangTilEnhet(id);
                    } catch (PepException e) {
                        throw new Feil(FeilType.SERVICE_UNAVAILABLE, "Kan ikke kontakte ABAC for utleding av kontorsperre.");
                    }
                })
                .orElse(true);
        FunksjonelleMetrikker.reportFilterAktivitet(aktivitet, hasAccess);
        return hasAccess;
    }

    private Person sjekkTilgangTilPerson(Person person) {
        if (person instanceof Person.Fnr) {
            return Person.fnr(pepClient.sjekkLeseTilgangTilFnr(person.get()));
        } else if (person instanceof Person.AktorId) {
            return sjekkTilgangTilPerson(brukerService.getFNRForAktorId((Person.AktorId) person).orElseThrow(IngenTilgang::new));
        } else {
            throw new IngenTilgang();
        }
    }
}
