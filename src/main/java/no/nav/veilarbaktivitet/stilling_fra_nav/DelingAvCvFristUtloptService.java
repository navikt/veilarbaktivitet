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
public class DelingAvCvFristUtloptService {

    private final DelingAvCvService delingAvCvService;
    private final DelingAvCvDAO delingAvCvDAO;

    @Timed(value = "avsluttUtloptStillingFraNavAktiviteter", histogram = true)
    public int avsluttUtlopedeAktiviteter(int maxAntall) {
        List<AktivitetData> aktivitetDataer = delingAvCvDAO.hentStillingFraNavDerFristErUtlopt(maxAntall);

        aktivitetDataer.forEach(aktivitet -> {
            try {
                delingAvCvService.avsluttAktivitet(aktivitet, Person.navIdent("SYSTEM"));
            } catch (Exception e) {
                log.error("Kunne ikke avslutte utlopede aktiviteter", e);
            }
        });

        return aktivitetDataer.size();
    }
}
