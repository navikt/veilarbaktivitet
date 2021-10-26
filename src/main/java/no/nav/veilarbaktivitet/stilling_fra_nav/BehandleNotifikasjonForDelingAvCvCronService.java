package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.RequiredArgsConstructor;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.veilarbaktivitet.brukernotifikasjon.Brukernotifikasjon;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselFunksjon;
import no.nav.veilarbaktivitet.brukernotifikasjon.kvitering.KvitteringDAO;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class BehandleNotifikasjonForDelingAvCvCronService {

    private final LeaderElectionClient leaderElectionClient;
    private final KvitteringDAO kvitteringsDao;
    private final BehandleNotifikasjonForDelingAvCvService behandleNotifikasjonForDelingAvCvService;

    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    )
    public void behandleFerdigstilteNotifikasjoner() {
        if (leaderElectionClient.isLeader()) {
            while (behandleFerdigstilteNotifikasjoner(500) == 500) ;
        }
    }

    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    )
    public void behandleFeiledeNotifikasjoner() {
        if (leaderElectionClient.isLeader()) {
            while (behandleFeiledeNotifikasjoner(500) == 500) ;
        }
    }

    public int behandleFerdigstilteNotifikasjoner(int maksAntall) {
        List<Brukernotifikasjon> brukernotifikasjonList = kvitteringsDao.hentFullfortIkkeBehandlet(maksAntall, VarselFunksjon.DELING_AV_CV);
        brukernotifikasjonList.stream().forEach(behandleNotifikasjonForDelingAvCvService::behandleFerdigstiltKvittering);
        return brukernotifikasjonList.size();
    }

    public int behandleFeiledeNotifikasjoner(int maksAntall) {
        List<Brukernotifikasjon> brukernotifikasjonList = kvitteringsDao.hentFeiletIkkeBehandlet(maksAntall, VarselFunksjon.DELING_AV_CV);
        brukernotifikasjonList.stream().forEach(behandleNotifikasjonForDelingAvCvService::behandleFeiletKvittering);
        return brukernotifikasjonList.size();
    }

}
