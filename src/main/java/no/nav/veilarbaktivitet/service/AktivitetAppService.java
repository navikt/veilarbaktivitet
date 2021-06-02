package no.nav.veilarbaktivitet.service;

import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.veilarbaktivitet.arena.ArenaService;
import no.nav.veilarbaktivitet.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AktivitetAppService {

    private final ArenaService arenaService;
    private final AuthService authService;
    private final AktivitetService aktivitetService;
    private final MetricService metricService;

    private static final Set<AktivitetTypeData> TYPER_SOM_KAN_ENDRES_EKSTERNT = new HashSet<>(Arrays.asList(
            AktivitetTypeData.EGENAKTIVITET,
            AktivitetTypeData.JOBBSOEKING,
            AktivitetTypeData.SOKEAVTALE,
            AktivitetTypeData.IJOBB,
            AktivitetTypeData.BEHANDLING
    ));

    private static final Set<AktivitetTypeData> TYPER_SOM_KAN_OPPRETTES_EKSTERNT = new HashSet<>(Arrays.asList(
            AktivitetTypeData.BEHANDLING,
            AktivitetTypeData.EGENAKTIVITET,
            AktivitetTypeData.JOBBSOEKING,
            AktivitetTypeData.IJOBB
    ));

    public List<AktivitetData> hentAktiviteterForIdent(Person ident) {
        authService.sjekkTilgangTilPerson(ident);
        List<AktivitetData> aktiviteter = authService.getAktorIdForPersonBrukerService(ident)
                .map(aktivitetService::hentAktiviteterForAktorId)
                .orElseThrow(RuntimeException::new);
        return filterKontorsperret(aktiviteter);
    }

    public AktivitetData hentAktivitet(long id) {
        AktivitetData aktivitetData = aktivitetService.hentAktivitetMedForhaandsorientering(id);
        settLestAvBrukerHvisUlest(aktivitetData);
        authService.sjekkTilgang(aktivitetData.getAktorId(), aktivitetData.getKontorsperreEnhetId());
        return aktivitetData;
    }

    public List<AktivitetData> hentAktivitetVersjoner(long id) {
        hentAktivitet(id); // innebærer tilgangskontroll
        return aktivitetService.hentAktivitetVersjoner(id)
                .stream()
                .filter(this::erEksternBrukerOgEndringenSkalVereSynnelig)
                .collect(Collectors.toList());
    }

    public void settLestAvBrukerHvisUlest(AktivitetData aktivitetData) {
        if (authService.erEksternBruker() && aktivitetData.getLestAvBrukerForsteGang() == null) {
            AktivitetData hentetAktivitet = aktivitetService.settLestAvBrukerTidspunkt(aktivitetData.getId());
            metricService.reportAktivitetLestAvBrukerForsteGang(hentetAktivitet);
        }
    }

    private boolean erEksternBrukerOgEndringenSkalVereSynnelig(AktivitetData aktivitetData) {
        return !authService.erEksternBruker() || erSynligForEksterne(aktivitetData);
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
        authService.sjekkTilgangTilPerson(ident);

        if (authService.erEksternBruker() && !TYPER_SOM_KAN_OPPRETTES_EKSTERNT.contains(aktivitetData.getAktivitetType())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Eksternbruker kan ikke opprette denne aktivitetstypen. Fikk: " + aktivitetData.getAktivitetType());
        }

        Person.AktorId aktorId = authService.getAktorIdForPersonBrukerService(ident).orElseThrow(RuntimeException::new);
        Person loggedInUser = authService.erEksternBruker() ?
                aktorId :
                authService.getLoggedInnUser().orElseThrow(RuntimeException::new);

        long id = aktivitetService.opprettAktivitet(aktorId, aktivitetData, loggedInUser);
        return this.hentAktivitet(id); // this is done because of KVP
    }

    @Transactional
    public AktivitetData oppdaterAktivitet(AktivitetData aktivitet) {
        AktivitetData original = hentAktivitet(aktivitet.getId()); // innebærer tilgangskontroll
        kanEndreAktivitetGuard(original, aktivitet);

        Person loggedInnUser = authService.getLoggedInnUser().orElseThrow(RuntimeException::new);

        if (authService.erInternBruker()) {
            if (original.isAvtalt()) {
                if (original.getAktivitetType() == AktivitetTypeData.MOTE) {
                    aktivitetService.oppdaterMoteTidStedOgKanal(original, aktivitet, loggedInnUser);
                } else {
                    aktivitetService.oppdaterAktivitetFrist(original, aktivitet, loggedInnUser);
                }
            } else {
                aktivitetService.oppdaterAktivitet(original, aktivitet, loggedInnUser);
            }

            return aktivitetService.hentAktivitetMedForhaandsorientering(aktivitet.getId());

        } else if (authService.erEksternBruker()) {
            if (original.isAvtalt() || !TYPER_SOM_KAN_ENDRES_EKSTERNT.contains(original.getAktivitetType())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Feil aktivitetstype " + original.getAktivitetType());
            }

            aktivitetService.oppdaterAktivitet(original, aktivitet, loggedInnUser);
            return aktivitetService.hentAktivitetMedForhaandsorientering(aktivitet.getId());
        }

        // not a valid user
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    private void kanEndreAktivitetGuard(AktivitetData orginalAktivitet, AktivitetData aktivitet) {
        if (!Objects.equals(orginalAktivitet.getVersjon(), aktivitet.getVersjon())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        } else if (skalIkkeKunneEndreAktivitet(orginalAktivitet)) {
            throw new IllegalArgumentException(
                    String.format("Kan ikke endre aktivitet [%s] i en ferdig status",
                            orginalAktivitet.getId())
            );
        }
    }

    private void kanEndreAktivitetEtikettGuard(AktivitetData orginalAktivitet, AktivitetData aktivitet) {
        if (!Objects.equals(orginalAktivitet.getVersjon(), aktivitet.getVersjon())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        } else if (skalIkkeKunneEndreAktivitetEtikett(orginalAktivitet)) {
            throw new IllegalArgumentException(
                    String.format("Kan ikke endre etikett på historisk aktivitet [%s]",
                            orginalAktivitet.getId())
            );
        }
    }

    private boolean skalIkkeKunneEndreAktivitet(AktivitetData aktivitetData) {
        AktivitetStatus status = aktivitetData.getStatus();
        return AktivitetStatus.AVBRUTT.equals(status)
                || AktivitetStatus.FULLFORT.equals(status)
                || aktivitetData.getHistoriskDato() != null
                || aktivitetData.getAktivitetType() == AktivitetTypeData.STILLING_FRA_NAV;
    }

    private boolean skalIkkeKunneEndreAktivitetEtikett(AktivitetData aktivitetData) {
        return aktivitetData.getHistoriskDato() != null;
    }

    @Transactional
    public AktivitetData oppdaterStatus(AktivitetData aktivitet) {
        val originalAktivitet = hentAktivitet(aktivitet.getId()); // innebærer tilgangskontroll
        kanEndreAktivitetGuard(originalAktivitet, aktivitet);

        Person endretAv = authService
                .getLoggedInnUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));

        if (authService.erEksternBruker() && !TYPER_SOM_KAN_ENDRES_EKSTERNT.contains(originalAktivitet.getAktivitetType())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        aktivitetService.oppdaterStatus(originalAktivitet, aktivitet, endretAv);
        var nyAktivitet = aktivitetService.hentAktivitetMedForhaandsorientering(originalAktivitet.getId());
        metricService.oppdatertStatus(nyAktivitet, authService.erInternBruker());

        return nyAktivitet;
    }

    @Transactional
    public AktivitetData oppdaterEtikett(AktivitetData aktivitet) {
        val originalAktivitet = hentAktivitet(aktivitet.getId()); // innebærer tilgangskontroll
        kanEndreAktivitetEtikettGuard(originalAktivitet, aktivitet);
        return authService.getLoggedInnUser()
                .map(userIdent -> {
                    aktivitetService.oppdaterEtikett(originalAktivitet, aktivitet, userIdent);
                    return aktivitetService.hentAktivitetMedForhaandsorientering(aktivitet.getId());
                })
                .orElseThrow(RuntimeException::new);
    }

    @Transactional
    public AktivitetData oppdaterReferat(AktivitetData aktivitet) {
        if (authService.erEksternBruker()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        val originalAktivitet = hentAktivitet(aktivitet.getId());
        kanEndreAktivitetGuard(originalAktivitet, aktivitet);

        aktivitetService.oppdaterReferat(
                originalAktivitet,
                aktivitet,
                authService.getLoggedInnUser().orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN))
        );

        return hentAktivitet(aktivitet.getId());
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
     * Checks the activity for KVP status, and returns true if the current user
     * can access the activity. If the activity is not tagged with KVP, true
     * is always returned.
     * <p>
     * This function reports real usage through the metric system.
     */
    private boolean canAccessKvpActivity(AktivitetData aktivitet) {
        return authService.sjekKvpTilgang(aktivitet.getKontorsperreEnhetId());
    }
}
