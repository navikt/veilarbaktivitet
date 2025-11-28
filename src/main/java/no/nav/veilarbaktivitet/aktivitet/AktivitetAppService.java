package no.nav.veilarbaktivitet.aktivitet;

import lombok.RequiredArgsConstructor;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.aktivitet.feil.EndringAvFerdigAktivitetException;
import no.nav.veilarbaktivitet.aktivitet.feil.EndringAvHistoriskAktivitetException;
import no.nav.veilarbaktivitet.eventsLogger.BigQueryClient;
import no.nav.veilarbaktivitet.eventsLogger.EventType;
import no.nav.veilarbaktivitet.oversikten.OversiktenService;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AktivitetAppService {

    private final Logger secureLog = LoggerFactory.getLogger("SecureLog");
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final IAuthService authService;
    private final AktivitetService aktivitetService;
    private final MetricService metricService;
    private final PersonService personService;
    private final BigQueryClient bigQueryClient;
    private final OversiktenService oversiktenService;

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

        return personService.getAktorIdForPersonBruker(ident)
                .map(aktivitetService::hentAktiviteterForAktorId)
                .orElseThrow(RuntimeException::new);
    }

    public List<AktivitetData> hentAktiviteterUtenKontorsperre(Person ident) {
        return hentAktiviteterForIdent(ident)
                .stream().filter(aktivitet -> {
                    var kontorsperreEnhetId = aktivitet.getKontorsperreEnhetId();
                    return kontorsperreEnhetId == null || kontorsperreEnhetId.isBlank();
                }).toList();
    }

    public AktivitetData hentAktivitet(long id) {
        AktivitetData aktivitetData = aktivitetService.hentAktivitetMedForhaandsorientering(id);
        settLestAvBrukerHvisUlest(aktivitetData);
        return aktivitetData;
    }

    public List<AktivitetData> hentAktivitetVersjoner(long id) {
        return aktivitetService.hentAktivitetVersjoner(id)
                .stream()
                .filter(this::erEksternBrukerOgEndringenSkalVereSynnelig)
                .toList();
    }

    private void settLestAvBrukerHvisUlest(AktivitetData aktivitetData) {
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

    private boolean referatErDeltMedBruker(AktivitetData nyAktivitet) {
        if(nyAktivitet.getMoteData() == null) return false;
        return nyAktivitet.getMoteData().isReferatPublisert();
    }

    private boolean nyopprettetAktivitetKanHaReferat(AktivitetData nyAktivitet) {
        var aktivitetstyperSomKanHaReferatNårAktivitetOpprettes = List.of(AktivitetTypeData.SAMTALEREFERAT);
        return aktivitetstyperSomKanHaReferatNårAktivitetOpprettes.contains(nyAktivitet.getAktivitetType());
    }

    @Transactional
    public AktivitetData opprettNyAktivitet(AktivitetData aktivitetData) {

        if (aktivitetData.getAktivitetType() == AktivitetTypeData.STILLING_FRA_NAV) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (authService.erEksternBruker() && !TYPER_SOM_KAN_OPPRETTES_EKSTERNT.contains(aktivitetData.getAktivitetType())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Eksternbruker kan ikke opprette denne aktivitetstypen. Fikk: " + aktivitetData.getAktivitetType());
        }

        AktivitetData nyAktivitet = aktivitetService.opprettAktivitet(aktivitetData);

        if (nyopprettetAktivitetKanHaReferat(nyAktivitet)) {
            if (referatErDeltMedBruker(nyAktivitet)) {
                bigQueryClient.logEvent(nyAktivitet, EventType.SAMTALEREFERAT_OPPRETTET_OG_DELT_MED_BRUKER);
            } else {
                bigQueryClient.logEvent(nyAktivitet, EventType.SAMTALEREFERAT_OPPRETTET);
                oversiktenService.lagreStartMeldingOmUdeltSamtalereferatIUtboks(nyAktivitet.getAktorId(), nyAktivitet.getId());
            }
        }

        // dette er gjort på grunn av KVP
        return authService.erSystemBruker() ? nyAktivitet.withKontorsperreEnhetId(null) : nyAktivitet;
    }

    @Transactional
    public AktivitetData oppdaterAktivitet(AktivitetData aktivitet) {
        AktivitetData original = hentAktivitet(aktivitet.getId());
        kanEndreAktivitetGuard(original, aktivitet.getVersjon(), aktivitet.getAktorId());
        if (original.getAktivitetType() == AktivitetTypeData.STILLING_FRA_NAV) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (authService.erInternBruker()) {
            oppdaterSomNav(aktivitet, original);
            return aktivitetService.hentAktivitetMedForhaandsorientering(aktivitet.getId());
        } else if (authService.erEksternBruker()) {
            oppdaterSomEksternBruker(aktivitet, original);
            return aktivitetService.hentAktivitetMedForhaandsorientering(aktivitet.getId());
        }

        // not a valid user
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    private void oppdaterSomNav(AktivitetData aktivitet, AktivitetData original) {
        if (original.isAvtalt()) {
            if (original.getAktivitetType() == AktivitetTypeData.MOTE) {
                aktivitetService.oppdaterMoteTidStedOgKanal(original, aktivitet);
            } else {
                aktivitetService.oppdaterAktivitetFrist(original, aktivitet);
            }
        } else {
            aktivitetService.oppdaterAktivitet(original, aktivitet);
        }
    }

    private void oppdaterSomEksternBruker(AktivitetData aktivitet, AktivitetData original) {
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
            aktivitetService.oppdaterAktivitetFrist(original, aktivitet);
        } else {
            aktivitetService.oppdaterAktivitet(original, aktivitet);
        }
    }

    private void kanEndreAktivitetGuard(AktivitetData orginalAktivitet, long sisteVersjon, Person.AktorId aktorId) {
        if (!orginalAktivitet.getAktorId().equals(aktorId)) {
            secureLog.error("kan ikke oppdatere aktorid på aktivitet: orginal aktorId: {}, aktorId fra context: {}, aktivitetsId: {}",
                    orginalAktivitet.getAktorId(), aktorId, orginalAktivitet.getId());
            throw new IllegalArgumentException("kan ikke oppdatere aktorid på aktivitet");
        }
        if (orginalAktivitet.getVersjon() != sisteVersjon) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        } else if (!orginalAktivitet.endringTillatt()) {
            log.warn(String.format("Kan ikke endre aktivitet [%s] i en ferdig status",
                    orginalAktivitet.getId()));
            throw new EndringAvFerdigAktivitetException("Kan ikke endre aktivitet i en ferdig status");
        }
    }

    private void kanEndreAktivitetEtikettGuard(AktivitetData orginalAktivitet, AktivitetData aktivitet) {
        if (!orginalAktivitet.getAktorId().equals(aktivitet.getAktorId())) {
            throw new IllegalArgumentException("kan ikke oppdatere aktorid på aktivitet");

        }
        if (!Objects.equals(orginalAktivitet.getVersjon(), aktivitet.getVersjon())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        } else if (orginalAktivitet.getHistoriskDato() != null) {
            // Etikett skal kunne endres selv om aktivitet er fullført eller avbrutt
            log.warn(String.format("Kan ikke endre etikett på historisk aktivitet [%s]", orginalAktivitet.getId()));
            throw new EndringAvHistoriskAktivitetException("Kan ikke endre etikett på historisk aktivitet");
        }
    }

    @Transactional
    public AktivitetData oppdaterStatus(AktivitetData aktivitet) {
        final var originalAktivitet = hentAktivitet(aktivitet.getId());
        kanEndreAktivitetGuard(originalAktivitet, aktivitet.getVersjon(), aktivitet.getAktorId());

        if (authService.erEksternBruker() && !TYPER_SOM_KAN_ENDRES_EKSTERNT.contains(originalAktivitet.getAktivitetType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        aktivitetService.oppdaterStatus(originalAktivitet, aktivitet);
        var nyAktivitet = aktivitetService.hentAktivitetMedForhaandsorientering(originalAktivitet.getId());
        metricService.oppdatertStatus(nyAktivitet, authService.erInternBruker());

        return nyAktivitet;
    }

    @Transactional
    public AktivitetData oppdaterEtikett(AktivitetData aktivitet) {
        final var originalAktivitet = hentAktivitet(aktivitet.getId());
        kanEndreAktivitetEtikettGuard(originalAktivitet, aktivitet);
        aktivitetService.oppdaterEtikett(originalAktivitet, aktivitet);
        return aktivitetService.hentAktivitetMedForhaandsorientering(aktivitet.getId());
    }

    @Transactional
    public AktivitetData oppdaterReferat(AktivitetData aktivitet) {
        final var originalAktivitet = hentAktivitet(aktivitet.getId());
        kanEndreAktivitetGuard(originalAktivitet, aktivitet.getVersjon(), aktivitet.getAktorId());

        var oppdatertAktivitet = aktivitetService.oppdaterReferat(originalAktivitet, aktivitet);

        var maybeEventType = hentEventTypeForReferat(originalAktivitet, aktivitet);
        maybeEventType.ifPresent(eventType -> {
            bigQueryClient.logEvent(oppdatertAktivitet, eventType);
            sendMeldingTilOversikten(oppdatertAktivitet, eventType);
        });
        return oppdatertAktivitet;
    }

    private void sendMeldingTilOversikten(AktivitetData aktivitet, EventType eventType) {
        if (eventType == EventType.SAMTALEREFERAT_OPPRETTET) { // Kan kun skje for aktivitetstype "Møte"
            oversiktenService.lagreStartMeldingOmUdeltSamtalereferatIUtboks(aktivitet.getAktorId(), aktivitet.getId());
        } else if (eventType == EventType.SAMTALEREFERAT_DELT_MED_BRUKER) {
            oversiktenService.lagreStoppMeldingOmUdeltSamtalereferatIUtboks(aktivitet.getAktorId(), aktivitet.getId());
        }
    }

    private Optional<EventType> hentEventTypeForReferat(AktivitetData originalAktivitet, AktivitetData oppdatertAktivitet) {
        var forrigeReferat = Optional.ofNullable(originalAktivitet.getMoteData()).map(it -> it.getReferat()).orElse("");
        var nesteReferat = Optional.ofNullable(oppdatertAktivitet.getMoteData()).map(it -> it.getReferat()).orElse("");

        var referatHarNåFåttInnhold = forrigeReferat.isEmpty() && !nesteReferat.isEmpty();
        var referatHarNåBlittDeltMedBruker = !originalAktivitet.getMoteData().isReferatPublisert() && oppdatertAktivitet.getMoteData().isReferatPublisert();

        if (referatHarNåFåttInnhold && !referatHarNåBlittDeltMedBruker) {
            return Optional.of(EventType.SAMTALEREFERAT_OPPRETTET);
        } else if (referatHarNåFåttInnhold && referatHarNåBlittDeltMedBruker) {
            return Optional.of(EventType.SAMTALEREFERAT_OPPRETTET_OG_DELT_MED_BRUKER);
        } else if (!referatHarNåFåttInnhold && referatHarNåBlittDeltMedBruker) {
            return Optional.of(EventType.SAMTALEREFERAT_DELT_MED_BRUKER);
        } else {
            return Optional.empty();
        }
    }
}
