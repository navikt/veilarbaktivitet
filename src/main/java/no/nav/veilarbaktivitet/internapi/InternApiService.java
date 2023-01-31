package no.nav.veilarbaktivitet.internapi;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.EnhetId;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;

import no.nav.veilarbaktivitet.person.Person;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InternApiService {
    private final IAuthService authService;
    private final AktivitetDAO aktivitetDAO;

    public List<AktivitetData> hentAktiviteter(String aktorId) {
        if (StringUtils.isEmpty(aktorId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (authService.erEksternBruker()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        authService.sjekkTilgangTilPerson(Person.aktorId(aktorId).eksternBrukerId());
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
        if (aktivitet.getKontorsperreEnhetId() == null) return true;
        return authService.harTilgangTilEnhet(EnhetId.of(aktivitet.getKontorsperreEnhetId()));

    }
}
