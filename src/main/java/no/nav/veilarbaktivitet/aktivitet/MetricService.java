package no.nav.veilarbaktivitet.aktivitet;

import no.nav.common.log.MDCConstants;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.person.InnsenderData;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MetricService {

    private final MetricsClient metricsClient;
    private static final String MALID = "malId";
    private static final String AUTOMATISKOPPRETTET = "automatiskOpprettet";
    private static final String TYPE = "type";
    public static final String SOURCE = "source";

    public MetricService(MetricsClient metricsClient) {
        this.metricsClient = metricsClient;
    }


    private String getSource() {
        var mainSource = MDC.get(SOURCE);
        var logFilterSource = MDC.get(MDCConstants.MDC_CONSUMER_ID);
        return Optional.ofNullable(mainSource != null ? mainSource : logFilterSource).orElse("unknown");
    }

    public void opprettNyAktivitetMetrikk(AktivitetData aktivitetData) {
        String malId = Optional.ofNullable(aktivitetData.getMalid()).orElse("");
        Event ev = new Event("aktivitet.ny")
                .addTagToReport(TYPE, aktivitetData.getAktivitetType().toString())
                .addFieldToReport("lagtInnAvNAV", aktivitetData.getLagtInnAv().equals(InnsenderData.NAV))
                .addFieldToReport(AUTOMATISKOPPRETTET, aktivitetData.isAutomatiskOpprettet())
                .addFieldToReport(MALID, malId)
                .addFieldToReport(SOURCE, getSource());

        metricsClient.report(ev);
    }

    public void oppdaterAktivitetMetrikk(AktivitetData aktivitetData, boolean blittAvtalt, boolean erAutomatiskOpprettet) {
        String malId = Optional.ofNullable(aktivitetData.getMalid()).orElse("");
        Event ev = new Event("aktivitet.oppdatert")
                .addTagToReport(TYPE, aktivitetData.getAktivitetType().toString())
                .addFieldToReport("blittAvtalt", blittAvtalt)
                .addFieldToReport(AUTOMATISKOPPRETTET, erAutomatiskOpprettet)
                .addFieldToReport(MALID, malId)
                .addFieldToReport(SOURCE, getSource());
        metricsClient.report(ev);
    }

    public void reportAktivitetLestAvBrukerForsteGang(AktivitetData aktivitetData) {
        String malId = Optional.ofNullable(aktivitetData.getMalid()).orElse("");

        Event ev = new Event("aktivitet.lestAvBrukerForsteGang")
                .addTagToReport(TYPE, aktivitetData.getAktivitetType().toString())
                .addFieldToReport(AUTOMATISKOPPRETTET, aktivitetData.isAutomatiskOpprettet())
                .addFieldToReport("lestTidspunkt", aktivitetData.getLestAvBrukerForsteGang().getTime())
                .addFieldToReport("tidSidenOpprettet", tidMellomOpprettetOgLestForsteGang(aktivitetData))
                .addFieldToReport(MALID, malId);
        metricsClient.report(ev);

    }

    public void oppdatertStatus(AktivitetData aktivitetData, boolean oppdatertAvNAV) {
        String malId = Optional.ofNullable(aktivitetData.getMalid()).orElse("");
        Event ev = new Event("aktivitet.oppdatert.status")
                .addTagToReport(TYPE, aktivitetData.getAktivitetType().toString())
                .addFieldToReport("status", aktivitetData.getStatus())
                .addFieldToReport("oppdatertAvNAV", oppdatertAvNAV)
                .addFieldToReport("tidSidenOpprettet", tidMellomOpprettetOgOppdatert(aktivitetData))
                .addFieldToReport(AUTOMATISKOPPRETTET, aktivitetData.isAutomatiskOpprettet())
                .addFieldToReport(MALID, malId)
                .addFieldToReport(SOURCE, getSource());
        metricsClient.report(ev);
    }

    private static long tidMellomOpprettetOgOppdatert(AktivitetData aktivitetData) {
        return aktivitetData.getEndretDato().getTime() - aktivitetData.getOpprettetDato().getTime();
    }

    private static long tidMellomOpprettetOgLestForsteGang(AktivitetData aktivitetData) {
        return aktivitetData.getLestAvBrukerForsteGang().getTime() - aktivitetData.getOpprettetDato().getTime();
    }

}
