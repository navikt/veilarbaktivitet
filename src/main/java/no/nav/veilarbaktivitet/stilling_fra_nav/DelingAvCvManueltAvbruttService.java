package no.nav.veilarbaktivitet.stilling_fra_nav;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DelingAvCvManueltAvbruttService {

    private final DelingAvCvService delingAvCvService;
    private final DelingAvCvDAO delingAvCvDAO;

    @Timed(value = "stillingFraNavAvbruttEllerFullfortUtenSvar", histogram = true)
    public int notifiserFullfortEllerAvbruttUtenSvar(int maxantall) {
        List<AktivitetData> aktivitetData = delingAvCvDAO.hentStillingFraNavSomErFullfortEllerAvbruttUtenSvar(maxantall);
        aktivitetData.forEach(aktivitet -> {
            try {
                delingAvCvService.notifiserAvbruttEllerFullfortUtenSvar(aktivitet, Person.navIdent("SYSTEM"));
            } catch (Exception e) {
                log.warn("Behandling av fullført/avbrutt aktivitet aktivitetId={} feilet", aktivitet.getId());
                log.error("Kunne ikke behandle avbrutt/fullført aktivitet", e);
            }
        });
        return aktivitetData.size();
    }
}
