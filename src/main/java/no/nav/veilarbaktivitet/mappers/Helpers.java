package no.nav.veilarbaktivitet.mappers;

import lombok.NoArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.aktivitet_typer.EtikettTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.aktivitet_typer.JobbStatusTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.aktivitet_typer.JobbStatusTypeData;
import no.nav.veilarbaktivitet.aktivitet.aktivitet_typer.StillingsoekEtikettData;
import no.nav.veilarbaktivitet.aktivitet.base.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.base.AktivitetTypeData;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class Helpers {

    @NoArgsConstructor(access = PRIVATE)
    public static class Type {

        private static final BidiMap<AktivitetTypeData, AktivitetTypeDTO> typeMap = new DualHashBidiMap<>();

        static {
            typeMap.put(AktivitetTypeData.EGENAKTIVITET, AktivitetTypeDTO.EGEN);
            typeMap.put(AktivitetTypeData.JOBBSOEKING, AktivitetTypeDTO.STILLING);
            typeMap.put(AktivitetTypeData.SOKEAVTALE, AktivitetTypeDTO.SOKEAVTALE);
            typeMap.put(AktivitetTypeData.IJOBB, AktivitetTypeDTO.IJOBB);
            typeMap.put(AktivitetTypeData.BEHANDLING, AktivitetTypeDTO.BEHANDLING);
            typeMap.put(AktivitetTypeData.MOTE, AktivitetTypeDTO.MOTE);
            typeMap.put(AktivitetTypeData.SAMTALEREFERAT, AktivitetTypeDTO.SAMTALEREFERAT);
            typeMap.put(AktivitetTypeData.STILLING_FRA_NAV, AktivitetTypeDTO.STILLING_FRA_NAV);
        }

        public static AktivitetTypeData getData(AktivitetTypeDTO dto) {
            return typeMap.getKey(dto);
        }

        public static AktivitetTypeDTO getDTO(AktivitetTypeData data) {
            return typeMap.get(data);
        }

    }

    @NoArgsConstructor(access = PRIVATE)
    public static class Etikett {

        private static final BidiMap<StillingsoekEtikettData, EtikettTypeDTO> etikettMap = new DualHashBidiMap<>();

        static {
            etikettMap.put(StillingsoekEtikettData.AVSLAG, EtikettTypeDTO.AVSLAG);
            etikettMap.put(StillingsoekEtikettData.INNKALT_TIL_INTERVJU, EtikettTypeDTO.INNKALT_TIL_INTERVJU);
            etikettMap.put(StillingsoekEtikettData.JOBBTILBUD, EtikettTypeDTO.JOBBTILBUD);
            etikettMap.put(StillingsoekEtikettData.SOKNAD_SENDT, EtikettTypeDTO.SOKNAD_SENDT);
        }

        public static StillingsoekEtikettData getData(EtikettTypeDTO dto) {
            return etikettMap.getKey(dto);
        }

        public static EtikettTypeDTO getDTO(StillingsoekEtikettData data) {
            return etikettMap.get(data);
        }

    }

    @NoArgsConstructor(access = PRIVATE)
    public static class JobbStatus {

        private static final BidiMap<JobbStatusTypeData, JobbStatusTypeDTO> jobbStatusMap = new DualHashBidiMap<>();

        static {
            jobbStatusMap.put(JobbStatusTypeData.HELTID, JobbStatusTypeDTO.HELTID);
            jobbStatusMap.put(JobbStatusTypeData.DELTID, JobbStatusTypeDTO.DELTID);
        }

        public static JobbStatusTypeData getData(JobbStatusTypeDTO dto) {
            return jobbStatusMap.getKey(dto);
        }

        public static JobbStatusTypeDTO getDTO(JobbStatusTypeData data) {
            return jobbStatusMap.get(data);
        }

    }

}
