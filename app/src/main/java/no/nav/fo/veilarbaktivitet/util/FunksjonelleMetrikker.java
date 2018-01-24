package no.nav.fo.veilarbaktivitet.util;

import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.InnsenderData;
import no.nav.metrics.MetricsFactory;

public class FunksjonelleMetrikker {

    public static void opprettNyAktivitetMetrikk(AktivitetData aktivitetData) {
        MetricsFactory.createEvent("aktivitet.ny")
                .addFieldToReport("type", aktivitetData.getAktivitetType())
                .addFieldToReport("lagtInnAvNAV", aktivitetData.getLagtInnAv().equals(InnsenderData.NAV))
                .report();
    }

    public static void oppdaterAktivitetMetrikk(AktivitetData aktivitet, boolean blittAvtalt) {
        MetricsFactory.createEvent("aktivitet.oppdatert")
                .addFieldToReport("type", aktivitet.getAktivitetType())
                .addFieldToReport("blittAvtalt", blittAvtalt)
                .report();
    }

    public static void oppdatertStatusAvNAV(AktivitetData aktivitetData) {
        oppdatertStatus(aktivitetData, true);
    }

    public static void oppdatertStatusAvBruker(AktivitetData aktivitetData) {
        oppdatertStatus(aktivitetData, false);
    }

    private static void oppdatertStatus(AktivitetData aktivitetData, boolean oppdatertAvNAV) {
        MetricsFactory.createEvent("aktivitet.oppdatert.status")
                .addFieldToReport("status", aktivitetData)
                .addFieldToReport("oppdatertAvNAV", oppdatertAvNAV)
                .addFieldToReport("tidSidenOpprettet", tidMellomOpprettetOgOppdatert(aktivitetData))
                .report();
    }

    private static long tidMellomOpprettetOgOppdatert(AktivitetData aktivitetData) {
        return aktivitetData.getEndretDato().getTime() - aktivitetData.getOpprettetDato().getTime();
    }
}
