package no.nav.veilarbaktivitet.service;

import io.micrometer.core.annotation.Timed;
import no.nav.veilarbaktivitet.db.dao.MoteSmsDAO;
import no.nav.veilarbaktivitet.domain.SmsAktivitetData;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static java.util.UUID.randomUUID;

@Component
public class MoteSmsSenderService {

    private final VarselQueService varselQueue;
    private final MoteSmsDAO moteSmsDAO;
    private final TransactionTemplate transactionTemplate;

    public MoteSmsSenderService(VarselQueService varselQueue,
                                MoteSmsDAO moteSmsDAO, PlatformTransactionManager platformTransactionManager) {
        this.varselQueue = varselQueue;
        this.moteSmsDAO = moteSmsDAO;
        transactionTemplate = new TransactionTemplate(platformTransactionManager);
    }


    @Timed(value = "sendSingelServiceMelding", histogram = true)
    public void sendServiceMelding(SmsAktivitetData aktivitetData) {
        transactionTemplate.executeWithoutResult(aktivitetdata -> {
            String varselId = randomUUID().toString();
            moteSmsDAO.insertSmsSendt(aktivitetData, varselId);
            varselQueue.sendMoteSms(aktivitetData, varselId);
        });

    }
}
