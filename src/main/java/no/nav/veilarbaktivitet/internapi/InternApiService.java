package no.nav.veilarbaktivitet.internapi;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.person.AuthService;
import no.nav.veilarbaktivitet.person.Person;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InternApiService {
    private final AuthService authService;
    private final AktivitetDAO aktivitetDAO;

    public List<AktivitetData> hentAktiviteter(String aktorId) {
        if (StringUtils.isEmpty(aktorId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (authService.erEksternBruker()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        authService.sjekkTilgangTilPerson(Person.aktorId(aktorId));
        return aktivitetDAO.hentAktiviteterForAktorId(Person.aktorId(aktorId))
                .stream()
                .filter(this::erIkkeKontorsperret)
                .filter(this::erIkkeEksternAktivitet)
                .toList();
    }

    private boolean erIkkeEksternAktivitet(AktivitetData aktivitetData) {
        return aktivitetData.getAktivitetType() != AktivitetTypeData.EKSTERNAKTIVITET;
    }
    private boolean erIkkeKontorsperret(AktivitetData aktivitet) {
        return authService.sjekKvpTilgang(aktivitet.getKontorsperreEnhetId());

    }
}
