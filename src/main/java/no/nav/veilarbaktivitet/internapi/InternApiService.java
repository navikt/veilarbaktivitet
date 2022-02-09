package no.nav.veilarbaktivitet.internapi;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InternApiService {
    private final AktivitetDAO aktivitetDAO;

    public AktivitetData hentAktivitet(Integer aktivitetId) {
        return aktivitetDAO.hentAktivitet(aktivitetId);
    }

    public List<AktivitetData> hentAktiviteter(String aktorId, UUID oppfolgingsperiodeId) {
        if (aktorId == null && oppfolgingsperiodeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (oppfolgingsperiodeId == null) {
            return aktivitetDAO.hentAktiviteterForAktorId(Person.aktorId(aktorId));
        }

        if (aktorId == null) {
            return aktivitetDAO.hentAktiviteterForOppfolgingsperiodeId(oppfolgingsperiodeId);
        }

        return aktivitetDAO.hentAktiviteterForAktorId(Person.aktorId(aktorId))
                .stream()
                .filter(a -> a.getOppfolgingsperiodeId().toString().equals(oppfolgingsperiodeId.toString()))
                .toList();
    }
}
