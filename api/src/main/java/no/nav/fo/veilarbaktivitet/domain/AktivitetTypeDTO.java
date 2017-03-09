package no.nav.fo.veilarbaktivitet.domain;

import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Aktivitet;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.AktivitetType;

import java.util.HashMap;
import java.util.Map;

public enum AktivitetTypeDTO {
    EGEN,
    STILLING;

    private static final Map<AktivitetType, AktivitetTypeDTO> dtoMap = new HashMap<>();
    private static final Map<AktivitetTypeDTO, AktivitetType> typeMap = new HashMap<>();

    static {
        put(AktivitetType.EGENAKTIVITET, EGEN);
        put(AktivitetType.JOBBSOEKING, STILLING);
    }

    private static void put(AktivitetType aktivitetType, AktivitetTypeDTO aktivitetTypeDTO) {
        dtoMap.put(aktivitetType, aktivitetTypeDTO);
        typeMap.put(aktivitetTypeDTO, aktivitetType);
    }

    public static AktivitetTypeDTO getDTOType(Aktivitet aktivitet) {
        return dtoMap.get(aktivitet.getType());
    }

    public static AktivitetType getType(AktivitetTypeDTO aktivitetTypeDTO) {
        return typeMap.get(aktivitetTypeDTO);
    }

}
