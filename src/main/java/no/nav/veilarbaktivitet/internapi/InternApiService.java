package no.nav.veilarbaktivitet.internapi;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InternApiService {
    private final AktivitetDAO aktivitetDAO;

    public AktivitetData hentAktivitet(Integer aktivitetId) {
        return aktivitetDAO.hentAktivitet(aktivitetId);
    }

    public List<AktivitetData> hentAktiviteter(String aktorId) {
        return aktivitetDAO.hentAktiviteterForAktorId(Person.aktorId(aktorId));
    }
}
