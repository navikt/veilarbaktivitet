package no.nav.veilarbaktivitet.api;

import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetsplanDTO;
import no.nav.veilarbaktivitet.domain.EtikettTypeDTO;
import no.nav.veilarbaktivitet.domain.KanalDTO;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;

import javax.ws.rs.*;
import java.util.List;

import static java.util.Arrays.asList;


@Path("/aktivitet")
@Produces("application/json")
public interface AktivitetController {
    String ARENA_PREFIX = "ARENA";

    @GET
    AktivitetsplanDTO hentAktivitetsplan();

    @GET
    @Path("/arena")
    List<ArenaAktivitetDTO> hentArenaAktiviteter();

    @POST
    @Path("/ny")
    AktivitetDTO opprettNyAktivitet(AktivitetDTO aktivitet, @DefaultValue("false") @QueryParam("automatisk") boolean automatisk);

    @PUT
    @Path("/{id}")
    AktivitetDTO oppdaterAktivitet(AktivitetDTO aktivitet);

    @GET
    @Path("/{id}")
    AktivitetDTO hentAktivitet(@PathParam("id") String aktivitetId);

    @GET
    @Path("/etiketter")
    default List<EtikettTypeDTO> hentEtiketter() {
        return asList(EtikettTypeDTO.values());
    }

    @GET
    @Path("/kanaler")
    default List<KanalDTO> hentKanaler() {
        return asList(KanalDTO.values());
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
    @Path("/{id}/versjoner")
    List<AktivitetDTO> hentAktivitetVersjoner(@PathParam("id") String aktivitetId);

}
