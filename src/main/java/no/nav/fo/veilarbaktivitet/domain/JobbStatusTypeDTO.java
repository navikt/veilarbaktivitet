package no.nav.fo.veilarbaktivitet.domain;

import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.JobbStatus;

import java.util.HashMap;
import java.util.Map;

public enum JobbStatusTypeDTO {
    HELTID,
    DELTID;

    private static final Map<JobbStatus, JobbStatusTypeDTO> dtoMap = new HashMap<>();
    private static final Map<JobbStatusTypeDTO, JobbStatus> typeMap = new HashMap<>();

    static {
        put(JobbStatus.HELTID, HELTID);
        put(JobbStatus.DELTID, DELTID);
    }

    private static void put(JobbStatus jobbStatus, JobbStatusTypeDTO jobbStatusDTO) {
        dtoMap.put(jobbStatus, jobbStatusDTO);
        typeMap.put(jobbStatusDTO, jobbStatus);
    }

    public static JobbStatusTypeDTO getDTOType(JobbStatus jobbStatus) {
        return dtoMap.get(jobbStatus);
    }

    public static JobbStatus getType(JobbStatusTypeDTO jobbStatusDTO) {
        return typeMap.get(jobbStatusDTO);
    }

}