package no.nav.veilarbaktivitet.service;

import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.InnsenderData;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MetricService {

    private final MetricsClient metricsClient;

    public MetricService(MetricsClient metricsClient) {
        this.metricsClient = metricsClient;
    }

    public void opprettNyAktivitetMetrikk(AktivitetData aktivitetData) {
        String malId = Optional.ofNullable(aktivitetData.getMalid()).orElse("");
        Event ev = new Event("aktivitet.ny")
                .addTagToReport("type", aktivitetData.getAktivitetType().toString())
                .addFieldToReport("lagtInnAvNAV", aktivitetData.getLagtInnAv().equals(InnsenderData.NAV))
                .addFieldToReport("automatiskOpprettet", aktivitetData.isAutomatiskOpprettet())
                .addFieldToReport("malId", malId);

        metricsClient.report(ev);
    }

    public void oppdaterAktivitetMetrikk(AktivitetData aktivitetData, boolean blittAvtalt, boolean erAutomatiskOpprettet) {
        String malId = Optional.ofNullable(aktivitetData.getMalid()).orElse("");
        Event ev = new Event("aktivitet.oppdatert")
                .addTagToReport("type", aktivitetData.getAktivitetType().toString())
                .addFieldToReport("blittAvtalt", blittAvtalt)
                .addFieldToReport("automatiskOpprettet", erAutomatiskOpprettet)
                .addFieldToReport("malId", malId);
        metricsClient.report(ev);
    }

    public void oppdatertStatusAvNAV(AktivitetData aktivitetData) {
        oppdatertStatus(aktivitetData, true);
    }

    public void oppdatertStatusAvBruker(AktivitetData aktivitetData) {
        oppdatertStatus(aktivitetData, false);
    }

    public void reportIngenTilgangGrunnetKontorsperre() {
        Event ev = new Event("aktivitet.kontorsperre.ikketilgang");
        metricsClient.report(ev);
    }

    public void reportFilterAktivitet(AktivitetData aktivitetData, boolean harTilgang) {
        Event ev = new Event("aktivitet.filter")
                .addTagToReport("kontorsperre", String.valueOf(aktivitetData.getKontorsperreEnhetId() != null))
                .addTagToReport("harTilgang", String.valueOf(harTilgang));

        metricsClient.report(ev);
    }

    public void reportAktivitetLestAvBrukerForsteGang(AktivitetData aktivitetData) {
        String malId = Optional.ofNullable(aktivitetData.getMalid()).orElse("");

        Event ev = new Event("aktivitet.lestAvBrukerForsteGang")
                .addTagToReport("type", aktivitetData.getAktivitetType().toString())
                .addFieldToReport("automatiskOpprettet", aktivitetData.isAutomatiskOpprettet())
                .addFieldToReport("lestTidspunkt", aktivitetData.getLestAvBrukerForsteGang().getTime())
                .addFieldToReport("tidSidenOpprettet", tidMellomOpprettetOgLestForsteGang(aktivitetData))
                .addFieldToReport("malId", malId);
        metricsClient.report(ev);

    }

    public void oppdatertStatus(AktivitetData aktivitetData, boolean oppdatertAvNAV) {
        String malId = Optional.ofNullable(aktivitetData.getMalid()).orElse("");
        Event ev = new Event("aktivitet.oppdatert.status")
                .addTagToReport("type", aktivitetData.getAktivitetType().toString())
                .addFieldToReport("status", aktivitetData.getStatus())
                .addFieldToReport("oppdatertAvNAV", oppdatertAvNAV)
                .addFieldToReport("tidSidenOpprettet", tidMellomOpprettetOgOppdatert(aktivitetData))
                .addFieldToReport("automatiskOpprettet", aktivitetData.isAutomatiskOpprettet())
                .addFieldToReport("malId", malId);
        metricsClient.report(ev);
    }

    private static long tidMellomOpprettetOgOppdatert(AktivitetData aktivitetData) {
        return aktivitetData.getEndretDato().getTime() - aktivitetData.getOpprettetDato().getTime();
    }

    private static long tidMellomOpprettetOgLestForsteGang(AktivitetData aktivitetData) {
        return aktivitetData.getLestAvBrukerForsteGang().getTime() - aktivitetData.getOpprettetDato().getTime();
    }

}
