package no.nav.veilarbaktivitet.mappers;

import no.nav.veilarbaktivitet.domain.*;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import java.util.Collections;

public class Helpers {
    public static final BidiMap<AktivitetTypeData, AktivitetTypeDTO> typeMap = new DualHashBidiMap<>();
    static {
        typeMap.put(AktivitetTypeData.EGENAKTIVITET, AktivitetTypeDTO.EGEN);
        typeMap.put(AktivitetTypeData.JOBBSOEKING, AktivitetTypeDTO.STILLING);
        typeMap.put(AktivitetTypeData.SOKEAVTALE, AktivitetTypeDTO.SOKEAVTALE);
        typeMap.put(AktivitetTypeData.IJOBB, AktivitetTypeDTO.IJOBB);
        typeMap.put(AktivitetTypeData.BEHANDLING, AktivitetTypeDTO.BEHANDLING);
        typeMap.put(AktivitetTypeData.MOTE, AktivitetTypeDTO.MOTE);
        typeMap.put(AktivitetTypeData.SAMTALEREFERAT, AktivitetTypeDTO.SAMTALEREFERAT);
    }

    public static final BidiMap<StillingsoekEtikettData, EtikettTypeDTO> etikettMap = new DualHashBidiMap<>();
    static {
        etikettMap.put(StillingsoekEtikettData.AVSLAG, EtikettTypeDTO.AVSLAG);
        etikettMap.put(StillingsoekEtikettData.INNKALT_TIL_INTERVJU, EtikettTypeDTO.INNKALT_TIL_INTERVJU);
        etikettMap.put(StillingsoekEtikettData.JOBBTILBUD, EtikettTypeDTO.JOBBTILBUD);
        etikettMap.put(StillingsoekEtikettData.SOKNAD_SENDT, EtikettTypeDTO.SOKNAD_SENDT);
    }

    public static final BidiMap<JobbStatusTypeData, JobbStatusTypeDTO> jobbStatusMap = new DualHashBidiMap<>();
    static {
        jobbStatusMap.put(JobbStatusTypeData.HELTID, JobbStatusTypeDTO.HELTID);
        jobbStatusMap.put(JobbStatusTypeData.DELTID, JobbStatusTypeDTO.DELTID);
    }

}
