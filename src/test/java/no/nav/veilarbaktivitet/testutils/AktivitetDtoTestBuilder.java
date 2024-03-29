package no.nav.veilarbaktivitet.testutils;

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.aktivitet.mappers.Helpers;

public class AktivitetDtoTestBuilder {

    public static AktivitetDTO nyAktivitet(AktivitetTypeDTO aktivitetTypeDTO) {
        //TODO implementer denne ordenltig
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyAktivitet(Helpers.Type.getData(aktivitetTypeDTO));
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        aktivitetDTO.setId(null);
        aktivitetDTO.setVersjon(null);
        return aktivitetDTO;
    }
}
