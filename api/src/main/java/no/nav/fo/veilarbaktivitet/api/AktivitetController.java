package no.nav.fo.veilarbaktivitet.api;

import no.nav.fo.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetsplanDTO;
import no.nav.fo.veilarbaktivitet.domain.EndringsloggDTO;
import no.nav.fo.veilarbaktivitet.domain.EtikettTypeDTO;
import no.nav.fo.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Etikett;

import javax.ws.rs.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Path("/aktivitet")
@Produces("application/json")
public interface AktivitetController {

    @GET
    AktivitetsplanDTO hentAktivitetsplan();

    @GET
    @Path("/arena")
    List<ArenaAktivitetDTO> hentArenaAktiviteter();

    @POST
    @Path("/ny")
    AktivitetDTO opprettNyAktivitet(AktivitetDTO aktivitet);

    @PUT
    @Path("/{id}")
    AktivitetDTO oppdaterAktiviet(AktivitetDTO aktivitet);

    @GET
    @Path("/{id}")
    AktivitetDTO hentAktivitet(@PathParam("id") String aktivitetId);

    @GET
    @Path("/etiketter")
    default List<EtikettTypeDTO> hentEtiketter() {
        return Arrays.stream(Etikett.values())
                .map(EtikettTypeDTO::getDtoType)
                .collect(Collectors.toList());
    }

    @PUT
    @Path("/{id}/etikett")
    AktivitetDTO oppdaterEtikett(AktivitetDTO aktivitet);


    @DELETE
    @Path("/{id}")
    void slettAktivitet(@PathParam("id") String id);

    @PUT
    @Path("/{id}/status")
    AktivitetDTO oppdaterStatus(AktivitetDTO aktivitet);

    @GET
    @Path("/{id}/endringslogg")
    List<EndringsloggDTO> hentEndringsLoggForAktivitetId(@PathParam("id") String aktivitetId);

}
