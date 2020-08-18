package no.nav.veilarbaktivitet.mappers;

import no.nav.veilarbaktivitet.domain.*;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

public class Helpers {
    public static final BidiMap<AktivitetTypeData, AktivitetTypeDTO> typeMap =
            new DualHashBidiMap<AktivitetTypeData, AktivitetTypeDTO>() {{
                put(AktivitetTypeData.EGENAKTIVITET, AktivitetTypeDTO.EGEN);
                put(AktivitetTypeData.JOBBSOEKING, AktivitetTypeDTO.STILLING);
                put(AktivitetTypeData.SOKEAVTALE, AktivitetTypeDTO.SOKEAVTALE);
                put(AktivitetTypeData.IJOBB, AktivitetTypeDTO.IJOBB);
                put(AktivitetTypeData.BEHANDLING, AktivitetTypeDTO.BEHANDLING);
                put(AktivitetTypeData.MOTE, AktivitetTypeDTO.MOTE);
                put(AktivitetTypeData.SAMTALEREFERAT, AktivitetTypeDTO.SAMTALEREFERAT);
            }};


    public static final BidiMap<StillingsoekEtikettData, EtikettTypeDTO> etikettMap =
            new DualHashBidiMap<StillingsoekEtikettData, EtikettTypeDTO>() {{
                put(StillingsoekEtikettData.AVSLAG, EtikettTypeDTO.AVSLAG);
                put(StillingsoekEtikettData.INNKALT_TIL_INTERVJU, EtikettTypeDTO.INNKALT_TIL_INTERVJU);
                put(StillingsoekEtikettData.JOBBTILBUD, EtikettTypeDTO.JOBBTILBUD);
                put(StillingsoekEtikettData.SOKNAD_SENDT, EtikettTypeDTO.SOKNAD_SENDT);
            }};

    public static final BidiMap<JobbStatusTypeData, JobbStatusTypeDTO> jobbStatusMap =
            new DualHashBidiMap<JobbStatusTypeData, JobbStatusTypeDTO>() {{
                put(JobbStatusTypeData.HELTID, JobbStatusTypeDTO.HELTID);
                put(JobbStatusTypeData.DELTID, JobbStatusTypeDTO.DELTID);
            }};
}
