package no.nav.fo.veilarbaktivitet.domain;

import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Kanal;

import java.util.HashMap;
import java.util.Map;

public enum KanalDTO {
    OPPMOTE,
    TELEFON,
    INTERNETT
    ;

    private static final Map<Kanal, KanalDTO> dtoMap = new HashMap<>();
    private static final Map<KanalDTO, Kanal> typeMap = new HashMap<>();

    static {
        put(Kanal.INTERNETT, INTERNETT);
        put(Kanal.OPPMOTE, OPPMOTE);
        put(Kanal.TELEFON, TELEFON);
    }

    private static void put(Kanal kanal, KanalDTO kanalDTO) {
        dtoMap.put(kanal, kanalDTO);
        typeMap.put(kanalDTO, kanal);
    }

    public static KanalDTO getDTOType(Kanal jobbStatus) {
        return dtoMap.get(jobbStatus);
    }

    public static Kanal getType(KanalDTO jobbStatusDTO) {
        return typeMap.get(jobbStatusDTO);
    }
}
