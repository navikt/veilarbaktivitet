package no.nav.veilarbaktivitet.avtaltMedNav.varsel;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AvtaltVarselService {
    private final AvtaltVarselHandler avtaltVarselHandler;
    private final AvtaltVarselRepository repository;

    @Timed(value = "sendAvtaltVarsel", longTask = true, histogram = true)
    public void sendVarsel() {
        repository.hentVarslerSkalSendes(5000)
                .forEach(this::trySend);
    }

    @Timed(value = "stoppAvtaleVarsel", longTask = true, histogram = true)
    public void stoppVarsel() {
        repository.hentVarslerSomSkalStoppes(5000)
                .forEach(this::tryStop);
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
