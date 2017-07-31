package no.nav.fo.veilarbaktivitet.provider;

import lombok.AllArgsConstructor;
import no.nav.fo.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTransaksjonsType;
import no.nav.fo.veilarbaktivitet.domain.MoteData;
import no.nav.fo.veilarbaktivitet.service.AktivitetRSAppService;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTransaksjonsType.REFERAT_ENDRET;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTransaksjonsType.REFERAT_PUBLISERT;
import static no.nav.fo.veilarbaktivitet.domain.MoteData.builder;
import static no.nav.fo.veilarbaktivitet.mappers.AktivitetDTOMapper.mapTilAktivitetDTO;

@AllArgsConstructor
public class ReferatRessurs {

    private final long aktivitetId;
    private final AktivitetRSAppService aktivitetRSAppService;

    @PUT
    public AktivitetDTO oppdaterReferat(AktivitetDTO aktivitetDTO) {
        return oppdaterReferat(REFERAT_ENDRET, moteData -> moteData.withReferat(aktivitetDTO.referat));
    }

    @PUT
    @Path("/publiser")
    public AktivitetDTO publiserReferat(AktivitetDTO aktivitetDTO) {
        return oppdaterReferat(REFERAT_PUBLISERT, moteData -> moteData.withReferatPublisert(true));
    }

    private AktivitetDTO oppdaterReferat(AktivitetTransaksjonsType aktivitetTransaksjonsType, Function<MoteData, MoteData> moteDataFunction) {
        AktivitetData aktivitetData = aktivitetRSAppService.hentAktivitet(aktivitetId);
        MoteData moteData = moteDataFunction.apply(ofNullable(aktivitetData.getMoteData()).orElseGet(() -> builder().build()));
        return mapTilAktivitetDTO(aktivitetRSAppService.oppdaterReferat(aktivitetData.withMoteData(moteData), aktivitetTransaksjonsType));
    }

}
