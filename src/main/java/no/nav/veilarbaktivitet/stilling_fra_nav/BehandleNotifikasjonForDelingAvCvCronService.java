package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAktivitetIder;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.brukernotifikasjon.kvitering.KvitteringDAO;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class BehandleNotifikasjonForDelingAvCvCronService {

    private final KvitteringDAO kvitteringsDao;
    private final BehandleNotifikasjonForDelingAvCvService behandleNotifikasjonForDelingAvCvService;

    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    )
    @SchedulerLock(name = "deling_av_cv_behandleFerdigstilteNotifikasjoner", lockAtMostFor = "PT20M")
    public void behandleFerdigstilteNotifikasjoner() {
        while (behandleFerdigstilteNotifikasjoner(500) == 500) ;
    }

    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    )
    @SchedulerLock(name = "deling_av_cv_behandleFeiledeNotifikasjoner", lockAtMostFor = "PT20M")
    public void behandleFeiledeNotifikasjoner() {
        while (behandleFeiledeNotifikasjoner(500) == 500) ;
    }

    public int behandleFerdigstilteNotifikasjoner(int maksAntall) {
        List<BrukernotifikasjonAktivitetIder> brukernotifikasjonList = kvitteringsDao.hentFullfortIkkeBehandletForAktiviteter(maksAntall, VarselType.STILLING_FRA_NAV);
        brukernotifikasjonList.forEach(behandleNotifikasjonForDelingAvCvService::behandleFerdigstiltKvittering);
        return brukernotifikasjonList.size();
    }

    public int behandleFeiledeNotifikasjoner(int maksAntall) {
        List<BrukernotifikasjonAktivitetIder> brukernotifikasjonList = kvitteringsDao.hentFeiletIkkeBehandlet(maksAntall, VarselType.STILLING_FRA_NAV);
        brukernotifikasjonList.forEach(behandleNotifikasjonForDelingAvCvService::behandleFeiletKvittering);
        return brukernotifikasjonList.size();
    }

}
