package no.nav.veilarbaktivitet.avtaltMedNav.varsel;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.avtaltMedNav.ForhaandsorienteringDAO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.UUID.randomUUID;

@Service
@RequiredArgsConstructor
public class AvtaltVarselHandler {

    @Value("${app.env.aktivitetsplan.basepath}")
    private String aktivitetsplanBasepath;

    private final AvtaltVarselMQClient client;
    private final ForhaandsorienteringDAO dao;

    @Timed
    @Transactional
    public void handleSendVarsel(VarselIdHolder varselIdHolder) {
        final var varselBestillingId = randomUUID().toString();
        final var aktorId = varselIdHolder.getAktorId();

        dao.markerVarselSomSendt(varselIdHolder.getId(), varselBestillingId);
        client.sendVarsel(aktorId, varselBestillingId, createUrl(varselIdHolder));
    }

    @Timed
    @Transactional
    public void handleStopVarsel(String varselId) {
        dao.markerVareslStoppetSomSendt(varselId);
        client.stopRevarsel(varselId);
    }

    private String createUrl(VarselIdHolder ids) {
        ids.validate();

        final var aktivitetPath = aktivitetsplanBasepath + "/aktivitet/vis/";

        if (ids.getArenaaktivitetId() != null) {
            return aktivitetPath + ids.getArenaaktivitetId();
        }

        if (ids.getAktivitetId() != null) {
            return aktivitetPath + ids.getAktivitetId();
        }

        throw new IllegalStateException("Kan ikke generere URL, både arenaaktivitetid og aktivitetid er null");

    }

}
