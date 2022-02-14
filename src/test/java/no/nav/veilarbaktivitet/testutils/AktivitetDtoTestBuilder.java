package no.nav.veilarbaktivitet.testutils;

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.aktivitet.mappers.Helpers;

public class AktivitetDtoTestBuilder {

    public static AktivitetDTO nyAktivitet(AktivitetTypeDTO aktivitetTypeDTO) {
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyAktivitet(Helpers.Type.getData(aktivitetTypeDTO));
        return  AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
    }
}
