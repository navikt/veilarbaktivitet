package no.nav.fo.veilarbaktivitet.util;

import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.InnsenderData;
import no.nav.metrics.MetricsFactory;

import java.util.Optional;

public class FunksjonelleMetrikker {

    public static void opprettNyAktivitetMetrikk(AktivitetData aktivitetData) {
        MetricsFactory.createEvent("aktivitet.ny")
                .addTagToReport("type", aktivitetData.getAktivitetType().toString())
                .addFieldToReport("lagtInnAvNAV", aktivitetData.getLagtInnAv().equals(InnsenderData.NAV))
                .addFieldToReport("automatiskOpprettet", aktivitetData.isAutomatiskOpprettet())
                .addFieldToReport("malId", aktivitetData.getMalid())
                .report();
    }

    public static void oppdaterAktivitetMetrikk(AktivitetData aktivitetData, boolean blittAvtalt, boolean erAutomatiskOpprettet) {
        String malId = Optional.ofNullable(aktivitetData.getMalid()).orElse("");
        MetricsFactory.createEvent("aktivitet.oppdatert")
                .addTagToReport("type", aktivitetData.getAktivitetType().toString())
                .addFieldToReport("blittAvtalt", blittAvtalt)
                .addFieldToReport("automatiskOpprettet", erAutomatiskOpprettet)
                .addFieldToReport("malId", malId)
                .report();
    }

    public static void oppdatertStatusAvNAV(AktivitetData aktivitetData) {
        oppdatertStatus(aktivitetData, true);
    }

    public static void oppdatertStatusAvBruker(AktivitetData aktivitetData) {
        oppdatertStatus(aktivitetData, false);
    }

    public static void reportIngenTilgangGrunnetKontorsperre() {
        MetricsFactory.createEvent("aktivitet.kontorsperre.ikketilgang").report();
    }

    public static void reportFilterAktivitet(AktivitetData aktivitetData, boolean harTilgang) {
        MetricsFactory.createEvent("aktivitet.filter")
                .addTagToReport("kontorsperre", String.valueOf(aktivitetData.getKontorsperreEnhetId() != null))
                .addTagToReport("harTilgang", String.valueOf(harTilgang))
                .report();
    }

    public static void reportAktivitetLestAvBrukerForsteGang(AktivitetData aktivitetData) {
        MetricsFactory.createEvent("aktivitet.lestAvBrukerForsteGang")
                .addTagToReport("type", aktivitetData.getAktivitetType().toString())
                .addFieldToReport("automatiskOpprettet", aktivitetData.isAutomatiskOpprettet())
                .addFieldToReport("lestTidspunkt", aktivitetData.getLestAvBrukerForsteGang().getTime())
                .addFieldToReport("tidSidenOpprettet", tidMellomOpprettetOgLestForsteGang(aktivitetData))
                .addFieldToReport("malId", aktivitetData.getMalid())
                .report();
    }

    private static void oppdatertStatus(AktivitetData aktivitetData, boolean oppdatertAvNAV) {
        MetricsFactory.createEvent("aktivitet.oppdatert.status")
                .addTagToReport("type", aktivitetData.getAktivitetType().toString())
                .addFieldToReport("status", aktivitetData.getStatus())
                .addFieldToReport("oppdatertAvNAV", oppdatertAvNAV)
                .addFieldToReport("tidSidenOpprettet", tidMellomOpprettetOgOppdatert(aktivitetData))
                .addFieldToReport("automatiskOpprettet", aktivitetData.isAutomatiskOpprettet())
                .addFieldToReport("malId", aktivitetData.getMalid())
                .report();
    }

    private static long tidMellomOpprettetOgOppdatert(AktivitetData aktivitetData) {
        return aktivitetData.getEndretDato().getTime() - aktivitetData.getOpprettetDato().getTime();
    }

    private static long tidMellomOpprettetOgLestForsteGang(AktivitetData aktivitetData) {
        return aktivitetData.getLestAvBrukerForsteGang().getTime() - aktivitetData.getOpprettetDato().getTime();
    }

}
