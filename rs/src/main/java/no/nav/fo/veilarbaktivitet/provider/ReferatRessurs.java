package no.nav.fo.veilarbaktivitet.provider;

import lombok.AllArgsConstructor;
import no.nav.fo.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTransaksjonsType;
import no.nav.fo.veilarbaktivitet.domain.MoteData;
import no.nav.fo.veilarbaktivitet.service.AktivitetAppService;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static no.nav.apiapp.util.StringUtils.nullOrEmpty;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTransaksjonsType.*;
import static no.nav.fo.veilarbaktivitet.domain.MoteData.builder;
import static no.nav.fo.veilarbaktivitet.mappers.AktivitetDTOMapper.mapTilAktivitetDTO;

@AllArgsConstructor
public class ReferatRessurs {

    private final long aktivitetId;
    private final AktivitetAppService appService;

    @PUT
    public AktivitetDTO oppdaterReferat(AktivitetDTO aktivitetDTO) {
        return oppdaterReferat(
                (moteData) -> nullOrEmpty(moteData.getReferat()) ? REFERAT_OPPRETTET : REFERAT_ENDRET ,
                moteData -> moteData.withReferat(aktivitetDTO.referat)
        );
    }

    @PUT
    @Path("/publiser")
    public AktivitetDTO publiserReferat(AktivitetDTO aktivitetDTO) {
        return oppdaterReferat(
                (moteData) -> REFERAT_PUBLISERT,
                moteData -> moteData.withReferatPublisert(true)
        );
    }

    private AktivitetDTO oppdaterReferat(Function<MoteData, AktivitetTransaksjonsType> aktivitetTransaksjonsType, Function<MoteData, MoteData> moteDataFunction) {
        //todo change me. this code sucks
        AktivitetData aktivitetData = appService.hentAktivitet(aktivitetId);
        MoteData originalMoteData = ofNullable(aktivitetData.getMoteData()).orElseGet(() -> builder().build());
        MoteData moteData = moteDataFunction.apply(originalMoteData);
        AktivitetTransaksjonsType transaksjonsType = aktivitetTransaksjonsType.apply(originalMoteData);
        return mapTilAktivitetDTO(appService.oppdaterReferat(aktivitetData.withMoteData(moteData), transaksjonsType));
    }

}
