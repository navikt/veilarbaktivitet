package no.nav.veilarbaktivitet.service;

import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.InnsenderData;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Optional;

@Component
public class FunksjonelleMetrikker {

    private final MetricsClient metricsClient;

    public FunksjonelleMetrikker(MetricsClient metricsClient) {
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
                .addFieldToReport("lestTidspunkt", getTime(aktivitetData.getLestAvBrukerForsteGang()))
                .addFieldToReport("tidSidenOpprettet", tidMellomOpprettetOgLestForsteGang(aktivitetData))
                .addFieldToReport("malId", malId);
        metricsClient.report(ev);

    }

    private void oppdatertStatus(AktivitetData aktivitetData, boolean oppdatertAvNAV) {
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
        return getTime(aktivitetData.getEndretDato()) - getTime(aktivitetData.getOpprettetDato());
    }

    private static long tidMellomOpprettetOgLestForsteGang(AktivitetData aktivitetData) {
        return getTime(aktivitetData.getLestAvBrukerForsteGang()) - getTime(aktivitetData.getOpprettetDato());
    }

    private static long getTime(ZonedDateTime date) {
        return Timestamp.valueOf(date.toLocalDateTime()).getTime();
    }

}
