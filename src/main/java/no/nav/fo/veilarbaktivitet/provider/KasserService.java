package no.nav.fo.veilarbaktivitet.provider;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

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

    @Value("${veilarb.kassering.identer:Z990322}")
    String godkjenteIdenter;

    @PUT
    @Path("/{aktivitetId}")
    public boolean kasserAktivitet(@PathParam("aktivitetId") String aktivitetId) {
        long id = Long.parseLong(aktivitetId);
        AktivitetData aktivitetData = aktivitetDAO.hentAktivitet(id);

        return kjorHvisTilgang(aktivitetData.getAktorId(), aktivitetId, () -> aktivitetDAO.kasserAktivitet(id));
    }

    private boolean kjorHvisTilgang(String aktorId, String id, Supplier<Boolean> fn) {
        String fnr = aktorService.getFnr(aktorId).orElseThrow(IngenTilgang::new);
        pep.sjekkSkriveTilgangTilFnr(fnr);

        String veilederIdent = SubjectHandler.getIdent().orElse(null);
        List<String> godkjente = Arrays.asList(godkjenteIdenter.split("[\\.\\s]"));
        if (!godkjente.contains(veilederIdent)) {
            log.error("[KASSERING] {} har ikke tilgang til kassering av {} aktivitet", veilederIdent, aktorId);
            throw new IngenTilgang(String.format("[KASSERING] %s har ikke tilgang til kassinger av %s aktivitet", veilederIdent, aktorId));
        }

        boolean updated = fn.get();

        log.info("[KASSERING] {} kasserte en aktivitet. AktoerId: {} aktivitet_id: {}", veilederIdent, aktorId, id);
        return updated;
    }
}
