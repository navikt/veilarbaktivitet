package no.nav.veilarbaktivitet.aktivitet;

import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.person.AuthService;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class AktivitetAppService {

    private final AuthService authService;
    private final AktivitetService aktivitetService;
    private final MetricService metricService;

    private static final Set<AktivitetTypeData> TYPER_SOM_KAN_ENDRES_EKSTERNT = new HashSet<>(Arrays.asList(
            AktivitetTypeData.EGENAKTIVITET,
            AktivitetTypeData.JOBBSOEKING,
            AktivitetTypeData.SOKEAVTALE,
            AktivitetTypeData.IJOBB,
            AktivitetTypeData.BEHANDLING,
            AktivitetTypeData.STILLING_FRA_NAV
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
                .toList();
    }

    @Transactional
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

    @Transactional
    public AktivitetData opprettNyAktivitet(Person ident, AktivitetData aktivitetData) {
        authService.sjekkTilgangTilPerson(ident);

        if (aktivitetData.getAktivitetType() == AktivitetTypeData.STILLING_FRA_NAV) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (authService.erEksternBruker() && !TYPER_SOM_KAN_OPPRETTES_EKSTERNT.contains(aktivitetData.getAktivitetType())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Eksternbruker kan ikke opprette denne aktivitetstypen. Fikk: " + aktivitetData.getAktivitetType());
        }

        Person.AktorId aktorId = authService.getAktorIdForPersonBrukerService(ident).orElseThrow(RuntimeException::new);
        Person loggedInUser = authService.erEksternBruker() ?
                aktorId :
                authService.getLoggedInnUser().orElseThrow(RuntimeException::new);

        AktivitetData nyAktivitet = aktivitetService.opprettAktivitet(aktorId, aktivitetData, loggedInUser);

        // dette er gjort på grunn av KVP
        return authService.erSystemBruker() ? nyAktivitet.withKontorsperreEnhetId(null) : nyAktivitet;
    }

    @Transactional
    public AktivitetData oppdaterAktivitet(AktivitetData aktivitet) {
        AktivitetData original = hentAktivitet(aktivitet.getId()); // innebærer tilgangskontroll
        kanEndreAktivitetGuard(original, aktivitet);
        if (original.getAktivitetType() == AktivitetTypeData.STILLING_FRA_NAV) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        Supplier<Person> loggedInnUser = () -> authService.getLoggedInnUser().orElseThrow(RuntimeException::new);

        if (authService.erInternBruker()) {
            oppdaterSomNav(aktivitet, original, loggedInnUser.get());

            return aktivitetService.hentAktivitetMedForhaandsorientering(aktivitet.getId());

        } else if (authService.erEksternBruker()) {
            oppdaterSomEksternBruker(aktivitet, original, loggedInnUser.get());

            return aktivitetService.hentAktivitetMedForhaandsorientering(aktivitet.getId());
        }

        // not a valid user
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    private void oppdaterSomNav(AktivitetData aktivitet, AktivitetData original, Person loggedInnUser) {
        if (original.isAvtalt()) {
            if (original.getAktivitetType() == AktivitetTypeData.MOTE) {
                aktivitetService.oppdaterMoteTidStedOgKanal(original, aktivitet, loggedInnUser);
            } else {
                aktivitetService.oppdaterAktivitetFrist(original, aktivitet, loggedInnUser);
            }
        } else {
            aktivitetService.oppdaterAktivitet(original, aktivitet, loggedInnUser);
        }
    }

    private void oppdaterSomEksternBruker(AktivitetData aktivitet, AktivitetData original, Person loggedInnUser) {
        boolean denneAktivitetstypenKanIkkeEndresEksternt = !TYPER_SOM_KAN_ENDRES_EKSTERNT.contains(original.getAktivitetType());

        // Når behandling er avtalt må vi begrense hva som kan oppdateres til kun sluttdato for behandlingen.
        // Når behandling ikke er avtalt, skal ekstern bruker ha mulighet til å endre flere ting.
        boolean skalOppdatereTilDatoForAvtaltMedisinskBehandling = original.isAvtalt() && original.getAktivitetType() == AktivitetTypeData.BEHANDLING;

        if (denneAktivitetstypenKanIkkeEndresEksternt) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Feil aktivitetstype " + original.getAktivitetType());
        }

        if (original.isAvtalt() && original.getAktivitetType() != AktivitetTypeData.BEHANDLING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aktivitet er avtalt " + original.getAktivitetType());
        }

        if (skalOppdatereTilDatoForAvtaltMedisinskBehandling) {
            aktivitetService.oppdaterAktivitetFrist(original, aktivitet, loggedInnUser);
        } else {
            aktivitetService.oppdaterAktivitet(original, aktivitet, loggedInnUser);
        }
    }

    private void kanEndreAktivitetGuard(AktivitetData orginalAktivitet, AktivitetData aktivitet) {
        if (!Objects.equals(orginalAktivitet.getVersjon(), aktivitet.getVersjon())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        } else if (!orginalAktivitet.endringTillatt()) {
            throw new IllegalArgumentException(
                    String.format("Kan ikke endre aktivitet [%s] i en ferdig status",
                            orginalAktivitet.getId())
            );
        }
    }

    private void kanEndreAktivitetEtikettGuard(AktivitetData orginalAktivitet, AktivitetData aktivitet) {
        if (!Objects.equals(orginalAktivitet.getVersjon(), aktivitet.getVersjon())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        } else if (orginalAktivitet.getHistoriskDato() != null) {
            // Etikett skal kunne endres selv om aktivitet er fullført eller avbrutt
            throw new IllegalArgumentException(
                    String.format("Kan ikke endre etikett på historisk aktivitet [%s]",
                            orginalAktivitet.getId())
            );
        }
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
                .toList();
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
