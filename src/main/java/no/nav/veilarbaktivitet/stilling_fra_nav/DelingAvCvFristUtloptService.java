package no.nav.veilarbaktivitet.stilling_fra_nav;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.Person;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DelingAvCvFristUtloptService {

    private final LeaderElectionClient leaderElectionClient;
    private final DelingAvCvService delingAvCvService;
    private final DelingAvCvDAO delingAvCvDAO;

    @Scheduled(fixedDelay = 1800000, initialDelay = 300000)
    void avsluttUtlopedeAktiviteter() {
        if (leaderElectionClient.isLeader()) {
            boolean ferdig = false;
            while (!ferdig) {
                ferdig = 500 < avsluttUtlopedeAktiviteter(500);
            }
        }
    }

    @Timed(value = "avsluttUtloptStillingFraNavAktiviteter", longTask = true, histogram = true)
    int avsluttUtlopedeAktiviteter(int maxAntall) {
        List<AktivitetData> aktivitetDataer = delingAvCvDAO.hentAktiviteterSomSkalAvbrytes(maxAntall);

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
