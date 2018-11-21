package no.nav.fo.veilarbaktivitet.provider;

import lombok.AllArgsConstructor;
import no.nav.fo.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.fo.veilarbaktivitet.mappers.AktivitetDTOMapper;
import no.nav.fo.veilarbaktivitet.mappers.AktivitetDataMapper;
import no.nav.fo.veilarbaktivitet.service.AktivitetAppService;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import java.util.Optional;

@AllArgsConstructor
public class ReferatRessurs {

    private final AktivitetAppService appService;

    @PUT
    public AktivitetDTO oppdaterReferat(AktivitetDTO aktivitetDTO) {
        return Optional.of(aktivitetDTO)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(appService::oppdaterReferat)
                .map(AktivitetDTOMapper::mapTilAktivitetDTO)
                .orElseThrow(RuntimeException::new);
    }

    @PUT
    @Path("/publiser")
    public AktivitetDTO publiserReferat(AktivitetDTO aktivitetDTO) {
        return oppdaterReferat(aktivitetDTO);
    }
}
