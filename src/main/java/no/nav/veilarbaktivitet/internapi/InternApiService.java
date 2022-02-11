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
        if (authService.erEksternBruker()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if (aktorId != null) {
            authService.sjekkTilgangTilPerson(Person.aktorId(aktorId));
        }

        if (aktorId == null && oppfolgingsperiodeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (oppfolgingsperiodeId == null) {
            List<AktivitetData> aktivitetData = aktivitetDAO.hentAktiviteterForAktorId(Person.aktorId(aktorId));
            authService.sjekkTilgangTilPerson(Person.aktorId(aktorId));
            return filtrerKontorsperret(aktivitetData);
        }

        if (aktorId == null) {
            return filtrerKontorsperret(aktivitetDAO.hentAktiviteterForOppfolgingsperiodeId(oppfolgingsperiodeId));
        }

        List<AktivitetData> aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(Person.aktorId(aktorId))
                .stream()
                .filter(a -> a.getOppfolgingsperiodeId().toString().equals(oppfolgingsperiodeId.toString()))
                .toList();
        return filtrerKontorsperret(aktiviteter);
    }

    private List<AktivitetData> filtrerKontorsperret(List<AktivitetData> aktiviteter) {
        return aktiviteter
                .stream()
                .filter(a -> a.getKontorsperreEnhetId() == null)
                .toList();
    }
}
