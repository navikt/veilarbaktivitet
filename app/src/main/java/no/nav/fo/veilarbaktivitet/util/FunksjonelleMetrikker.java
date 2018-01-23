package no.nav.fo.veilarbaktivitet.util;

import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatus;
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

    public static void oppdatertStatusAvNAV(AktivitetStatus status) {
        oppdatertStatus(status, true);
    }

    public static void oppdatertStatusAvBruker(AktivitetStatus status) {
        oppdatertStatus(status, false);
    }

    private static void oppdatertStatus(AktivitetStatus status, boolean oppdatertAvNAV) {
        MetricsFactory.createEvent("aktivitet.oppdatert.status")
                .addFieldToReport("status", status)
                .addFieldToReport("oppdatertAvNAV", oppdatertAvNAV)
                .report();
    }
}
