package no.nav.veilarbaktivitet.internapi;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
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
            return hentAktiviteter(oppfolgingsperiodeId);
        }

        if (aktorId != null) {
            return hentAktiviteter(aktorId);
        }

        throw new IllegalArgumentException();
    }

    private List<AktivitetData> hentAktiviteterFiltrert(String aktorId, UUID oppfolgingsperiodeId) {
        authService.sjekkTilgangTilPerson(Person.aktorId(aktorId));
        List<AktivitetData> aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(Person.aktorId(aktorId))
                .stream()
                .filter(a -> a.getOppfolgingsperiodeId().toString().equals(oppfolgingsperiodeId.toString()))
                .toList();
        return filtrerKontorsperret(aktiviteter);
    }

    private List<AktivitetData> hentAktiviteter(String aktorId) {
        authService.sjekkTilgangTilPerson(Person.aktorId(aktorId));
        return filtrerKontorsperret(aktivitetDAO.hentAktiviteterForAktorId(Person.aktorId(aktorId)));
    }

    private List<AktivitetData> hentAktiviteter(UUID oppfolgingsperiodeId) {
        List<AktivitetData> aktivitetData = aktivitetDAO.hentAktiviteterForOppfolgingsperiodeId(oppfolgingsperiodeId);
        if (aktivitetData.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        authService.sjekkTilgangTilPerson(Person.aktorId(aktivitetData.get(0).getAktorId()));
        return filtrerKontorsperret(aktivitetData);
    }

    private List<AktivitetData> filtrerKontorsperret(List<AktivitetData> aktiviteter) {
        return aktiviteter
                .stream()
                .filter( a -> authService.sjekKvpTilgang(a.getKontorsperreEnhetId()))
                .toList();
    }
}
