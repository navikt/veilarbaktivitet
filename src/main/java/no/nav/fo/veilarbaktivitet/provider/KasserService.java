package no.nav.fo.veilarbaktivitet.provider;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static no.nav.fo.veilarbaktivitet.ApplicationContext.VEILARB_KASSERING_IDENTER_PROPERTY;
import static no.nav.fo.veilarbaktivitet.mappers.AktivitetDTOMapper.mapTilAktivitetDTO;
import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;

@Slf4j
@Component
@Path("/kassering")
public class KasserService {

    @Inject
    private AktivitetDAO aktivitetDAO;

    @Inject
    private PepClient pep;

    @Inject
    private AktorService aktorService;

    private String godkjenteIdenter = getOptionalProperty(VEILARB_KASSERING_IDENTER_PROPERTY).orElse("");

    @GET
    @Path("/{aktivitetId}")
    public AktivitetDTO aktivietForKasering(@PathParam("aktivitetId") String aktivitetId) {
        String veilederindent = SubjectHandler.getIdent().orElse(null);
        AktivitetData aktivitetData = hentAktivitet(aktivitetId, veilederindent);

        return mapTilAktivitetDTO(aktivitetData);
    }

    @PUT
    @Path("/{aktivitetId}")
    public boolean kasserAktivitet(@PathParam("aktivitetId") String aktivitetId) {
        String veilederindent = SubjectHandler.getIdent().orElse(null);
        AktivitetData aktivitetData = hentAktivitet(aktivitetId, veilederindent);

        log.info("[KASSERING] {} kasserte en aktivitet. AktoerId: {} aktivitet_id: {}", veilederindent, aktivitetData.getAktorId(), aktivitetData.getId());

        return aktivitetDAO.kasserAktivitet(aktivitetData.getId());
    }

    private AktivitetData hentAktivitet(String aktivitetId, String veilederIdent) {
        long id = Long.parseLong(aktivitetId);
        AktivitetData aktivitetData = aktivitetDAO.hentAktivitet(id);
        String aktorId = aktivitetData.getAktorId();

        String fnr = aktorService.getFnr(aktorId).orElseThrow(IngenTilgang::new);
        pep.sjekkSkriveTilgangTilFnr(fnr);

        List<String> godkjente = Arrays.asList(godkjenteIdenter.split("[\\.\\s]"));
        if (!godkjente.contains(veilederIdent)) {
            log.error("[KASSERING] {} har ikke tilgang til kassering av {} aktivitet", veilederIdent, aktorId);
            throw new IngenTilgang(String.format("[KASSERING] %s har ikke tilgang til kassinger av %s aktivitet", veilederIdent, aktorId));
        }

        return aktivitetData;
    }
}
