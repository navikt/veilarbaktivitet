package no.nav.fo.veilarbaktivitet.domain;


import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Etikett;

import java.util.HashMap;
import java.util.Map;

public enum EtikettTypeDTO {
    SOKNAD_SENDT,
    INNKALT_TIL_INTERVJU,
    AVSLAG,
    JOBBTILBUD;

    private static final Map<Etikett, EtikettTypeDTO> dtoMap = new HashMap<>();
    private static final Map<EtikettTypeDTO, Etikett> etiketter = new HashMap<>();

    static {
        put(Etikett.AVSLAG, AVSLAG);
        put(Etikett.SOEKNAD_SENDT, SOKNAD_SENDT);
        put(Etikett.INNKALDT_TIL_INTERVJU, INNKALT_TIL_INTERVJU);
        put(Etikett.JOBBTILBUD, JOBBTILBUD);
    }

    private static void put(Etikett etikett, EtikettTypeDTO dto) {
        dtoMap.put(etikett, dto);
        etiketter.put(dto, etikett);
    }

    public static EtikettTypeDTO getDtoType(Etikett etikett) {
        return dtoMap.get(etikett);
    }

    public static Etikett getType(EtikettTypeDTO etikett) {
        return etiketter.get(etikett);
    }

}
