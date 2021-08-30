package no.nav.veilarbaktivitet.send_paa_kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@EnableScheduling
public class UtsattJobbService {
    private final JobbDAO jobbDAO;
    private final List<AktivitetsJobb> jobber;
    public void bestillUtsattJobb(long aktivitetsId, long aktivitetsVersjon, JobbType jobbType) {
        // lagre jobben
    }

    @Scheduled(fixedRate = 60000, initialDelay = 60000)
    public void utfoerJobber() {
        // hent jobber fra db
        // kall riktig AktivitetsJobb per rad
        // slett rad eller oppdater status på rad
    }

}
