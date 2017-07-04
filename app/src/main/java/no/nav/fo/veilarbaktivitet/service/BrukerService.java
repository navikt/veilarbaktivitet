package no.nav.fo.veilarbaktivitet.service;

import no.nav.apiapp.security.SubjectService;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.fo.veilarbaktivitet.ws.consumer.AktoerConsumer;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Optional;

@Component
public class BrukerService {

    private final SubjectService subjectService = new SubjectService();

    private final AktoerConsumer aktoerConsumer;

    @Inject
    public BrukerService(AktoerConsumer aktoerConsumer) {
        this.aktoerConsumer = aktoerConsumer;
    }

    public Optional<String> getAktorIdForFNR(String fnr) {
        return aktoerConsumer.hentAktoerIdForIdent(fnr);
    }

    public Optional<String> getFNRForAktorId(String fnr) {
        return aktoerConsumer.hentIdentForAktorId(fnr);
    }

    public Optional<String> getLoggedInnUser() {
        return subjectService.getIdentType().flatMap(type -> {
            if (IdentType.EksternBruker.equals(type)) return getAktorIdForEksternBruker();
            else if (IdentType.InternBruker.equals(type)) return subjectService.getUserId();
            else return Optional.empty();
        });

    }

    private Optional<String> getAktorIdForEksternBruker() {
        return subjectService.getUserId().flatMap(this::getAktorIdForFNR);
    }
}
