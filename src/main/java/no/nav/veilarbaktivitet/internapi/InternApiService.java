package no.nav.veilarbaktivitet.internapi;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.person.AuthService;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InternApiService {
    private final AuthService authService;
    private final AktivitetDAO aktivitetDAO;

    public AktivitetData hentAktivitet(Integer aktivitetId) {
        if (authService.erEksternBruker()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        AktivitetData aktivitetData = aktivitetDAO.hentAktivitet(aktivitetId);
        authService.sjekkTilgangTilPerson(AktorId.of(aktivitetData.getAktorId()));
        return aktivitetData.getKontorsperreEnhetId() == null ? aktivitetData : null;
    }

    public List<AktivitetData> hentAktiviteter(String aktorId, UUID oppfolgingsperiodeId) {
        if (aktorId == null && oppfolgingsperiodeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (authService.erEksternBruker()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if (aktorId != null && oppfolgingsperiodeId != null) {
            return hentAktiviteterFiltrert(aktorId, oppfolgingsperiodeId);
        }

        if (oppfolgingsperiodeId != null) {
            return hentAktiviteter(oppfolgingsperiodeId, Person.aktorId(aktorId));
        } else {
            return hentAktiviteter(aktorId);
        }
    }

    private List<AktivitetData> hentAktiviteterFiltrert(String aktorId, UUID oppfolgingsperiodeId) {
        List<AktivitetData> aktivitetData = aktivitetDAO.hentAktiviteterForAktorId(Person.aktorId(aktorId))
                .stream()
                .filter(a -> a.getOppfolgingsperiodeId().toString().equals(oppfolgingsperiodeId.toString()))
                .filter(this::erIkkeKontorsperret)
                .filter(this::erIkkeEksternAktivitet)
                .toList();
        if (!aktivitetData.isEmpty() ) {
            authService.sjekkTilgangTilPerson(Person.aktorId(aktivitetData.get(0).getAktorId()));
        }
        return aktivitetData;
    }

    private List<AktivitetData> hentAktiviteter(String aktorId) {

        authService.sjekkTilgangTilPerson(Person.aktorId(aktorId));
        return aktivitetDAO.hentAktiviteterForAktorId(Person.aktorId(aktorId))
                .stream()
                .filter(this::erIkkeKontorsperret)
                .filter(this::erIkkeEksternAktivitet)
                .toList();
    }

    private List<AktivitetData> hentAktiviteter(UUID oppfolgingsperiodeId, Person.AktorId aktorId) {
        authService.sjekkTilgangTilPerson(aktorId);
        return aktivitetDAO.hentAktiviteterForOppfolgingsperiodeId(oppfolgingsperiodeId)
                .stream()
                .filter(this::erIkkeKontorsperret)
                .filter(this::erIkkeEksternAktivitet)
                .toList();
    }

    private boolean erIkkeEksternAktivitet(AktivitetData aktivitetData) {
        return aktivitetData.getAktivitetType() != AktivitetTypeData.TILTAKSAKTIVITET;
    }
    private boolean erIkkeKontorsperret(AktivitetData aktivitet) {
        return authService.sjekKvpTilgang(aktivitet.getKontorsperreEnhetId());

    }
}
