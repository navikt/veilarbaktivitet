package no.nav.veilarbaktivitet.motesms;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static java.time.Duration.ofHours;

@Service
@Slf4j
@RequiredArgsConstructor
public class MoteSMSService {
    private final MoteSmsDAO moteSmsDAO;
    private final BrukernotifikasjonService brukernotifikasjonService;

    @Timed(value = "moteservicemelding", histogram = true)
    public void sendMoteSms() {
        sendServicemeldinger(ofHours(1), ofHours(24));
    }

    protected void sendServicemeldinger(Duration fra,Duration til) {
        moteSmsDAO.hentMoterUtenVarsel(fra, til, 5000)
                .forEach(it -> {
                    moteSmsDAO.insertGjeldendeSms(it);
                    brukernotifikasjonService.opprettVarselPaaAktivitet(
                            it.aktivitetId(),
                            it.aktitetVersion(),
                            it.aktorId(),
                            it.getDitNavTekst(),
                            VarselType.MOTE_SMS,
                            it.getEpostTitel(),
                            it.getEpostBody(),
                            it.getSmsTekst()
                    );
                });
    }

    public void stopMoteSms() {

        moteSmsDAO.hentMoterMedOppdatertTidEllerKanal(5000)
                .forEach(it -> {
                    brukernotifikasjonService.setDone(it, VarselType.MOTE_SMS);
                    moteSmsDAO.slettGjeldende(it); //TODO endre til send beskjed sms om flyttet møte + skal sende på nytt hvis møtet er mere enn 48 timer fremm i tid
                });

        moteSmsDAO.hentMoteSmsSomFantStedForMerEnd(Duration.ofDays(7)) //TODO Trenger vi denne? Holder det at bruker kan fjerne den og den forsvinner når aktiviteter er fulført/avbrut eller blir historisk
                .forEach(it -> {
                    brukernotifikasjonService.setDone(it, VarselType.MOTE_SMS);
                    moteSmsDAO.slettGjeldende(it);
                });
    }
}
