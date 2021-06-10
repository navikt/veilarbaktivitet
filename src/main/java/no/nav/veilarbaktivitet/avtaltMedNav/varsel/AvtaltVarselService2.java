package no.nav.veilarbaktivitet.avtaltMedNav.varsel;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.avtaltMedNav.ForhaandsorienteringDAO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.UUID.randomUUID;

@Service
@RequiredArgsConstructor
public class AvtaltVarselService2 {
    private final OppgaveService oppgaveService;
    private final VarselMedHandlingService varselMedHandlingService;
    private final StopRevarslingService stopRevarslingService;
    private final ForhaandsorienteringDAO dao;

    @Timed
    @Transactional
    void sendVarsel(Varsel varsel) {
        String varselBestillingId = randomUUID().toString();
        String aktorId = varsel.AKTOR_ID;

        dao.markerVarselSomSendt(varsel.ID, varselBestillingId);

        varselMedHandlingService.send(aktorId, varselBestillingId);
        oppgaveService.send(aktorId, varselBestillingId, "url");
    }

    @Timed
    @Transactional
    void stopVarsel(String varselId) {
        dao.markerVareslStoppetSomSendt(varselId);
        stopRevarslingService.stopRevarsel(varselId);
    }

}
