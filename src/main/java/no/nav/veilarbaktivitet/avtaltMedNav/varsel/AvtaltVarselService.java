package no.nav.veilarbaktivitet.avtaltMedNav.varsel;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.avtaltMedNav.ForhaandsorienteringDAO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AvtaltVarselService {
    private final AvtaltVarselHandler avtaltVarselHandler;
    private final ForhaandsorienteringDAO dao;

    @Timed(value = "sendAvtaltVarsel", longTask = true, histogram = true)
    public void sendVarsel() {
        log.info("sendt avtalt varsler");

        List<VarselIdHolder> ider = dao.hentVarslerSkalSendes(5000);
        ider.forEach(this::trySend);

        log.info("sendt {} varselr", ider.size());
    }

    @Timed(value = "stoppAvtaleVarsel", longTask = true, histogram = true)
    public void stoppVarsel() {
        log.info("stoppAvtaleVarsel");
        dao.setVarselStoppetForIkkeSendt();
        List<String> ider = dao.hentVarslerSomSkalStoppes(5000);
        ider.forEach(this::tryStop);
        log.info("stopet {} varsler", ider.size());
    }

    private void trySend(VarselIdHolder varselIdHolder) {
        try {
            avtaltVarselHandler.handleSendVarsel(varselIdHolder);
        } catch (Exception e) {
            log.error("feilet sending av varsel: {}", varselIdHolder.getAktivitetId(), e);
        }
    }

    private void tryStop(String id) {
        try {
            avtaltVarselHandler.handleStopVarsel(id);
        } catch (Exception e) {
            log.error("feilet stopping av varsel: {}", id, e);
        }
    }
}
