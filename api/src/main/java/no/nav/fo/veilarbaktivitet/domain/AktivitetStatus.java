package no.nav.fo.veilarbaktivitet.domain;

import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Status;

import java.util.HashMap;
import java.util.Map;

public enum AktivitetStatus {
    PLANLAGT,
    GJENNOMFORT,
    FULLFORT,
    BRUKER_ER_INTERESSERT,
    AVBRUTT
    ;

    private static final Map<Status, AktivitetStatus> aktivitetStatuser = new HashMap<>();
    private static final Map<AktivitetStatus, Status> wsStatuser = new HashMap<>();

    static {
        map(Status.AVBRUTT, AVBRUTT);
        map(Status.BRUKER_ER_INTERESSERT, BRUKER_ER_INTERESSERT);
        map(Status.FULLFOERT, FULLFORT);
        map(Status.GJENNOMFOERT, GJENNOMFORT);
        map(Status.PLANLAGT, PLANLAGT);
    }

    private static void map(Status status, AktivitetStatus aktivitetStatus) {
        aktivitetStatuser.put(status, aktivitetStatus);
        wsStatuser.put(aktivitetStatus, status);
    }

    public static AktivitetStatus aktivitetStatus(Status status) {
        return aktivitetStatuser.get(status);
    }

    public static Status wsStatus(AktivitetStatus aktivitetStatus) {
        return wsStatuser.get(aktivitetStatus);
    }

}
