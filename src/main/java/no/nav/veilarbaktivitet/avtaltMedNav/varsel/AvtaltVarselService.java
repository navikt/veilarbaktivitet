package no.nav.veilarbaktivitet.avtaltMedNav.varsel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.avtaltMedNav.ForhaandsorienteringDAO;
import no.nav.veilarbaktivitet.avtaltMedNav.Type;
import no.nav.veilarbaktivitet.db.Database;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;

import static java.util.UUID.randomUUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AvtaltVarselService {
    private final AvtaltVarselService2 avtaltVarselService2;
    private final ForhaandsorienteringDAO dao;

    public void sendVarsel() {
        List<Varsel> varsels = dao.hentVarslerSkalSendes(5000);

        varsels.forEach(this::trySend);

    }

    private void trySend(Varsel varsel) {
        try {
            avtaltVarselService2.sendVarsel(varsel);
        } catch (Exception e) {
            log.error("feilet sending av varsel: {}", varsel.AKTIVITET_ID, e);
        }

    }

    public void stoppVarsel() {
        List<String> varselIder = dao.hentVarslerSomSkalStoppes(5000);
        varselIder.forEach(this::tryStop);
    }

    private void tryStop(String id) {
        try {
            avtaltVarselService2.stopVarsel(id);
        } catch (Exception e) {
            log.error("feilet stopping av varsel: {}", id, e);
        }
    }
}
