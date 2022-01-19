package no.nav.veilarbaktivitet.testutils;

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.aktivitet.mappers.Helpers;

public class AktivitetDTOTestBuilder {
    public static AktivitetDTO nyAktivitet(AktivitetTypeDTO type) {
        AktivitetTypeData dataType = Helpers.Type.getData(type);
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyAktivitet(dataType);
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        aktivitetDTO.setId(null);
        aktivitetDTO.setVersjon(null);
        aktivitetDTO.setStatus(null);
        aktivitetDTO.setOpprettetDato(null);
        aktivitetDTO.setEndretAv(null);

        return aktivitetDTO;
    }
}