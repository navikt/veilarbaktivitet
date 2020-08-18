package no.nav.veilarbaktivitet.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.types.feil.IngenTilgang;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.service.AuthService;
import org.springframework.stereotype.Component;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static no.nav.common.utils.EnvironmentUtils.getOptionalProperty;
import static no.nav.veilarbaktivitet.config.ApplicationContext.VEILARB_KASSERING_IDENTER_PROPERTY;

@Slf4j
@Component
@Path("/api/kassering")
public class KasserController {

    private final AktivitetDAO aktivitetDAO;
    private final AuthService auth;

    private final String godkjenteIdenter = getOptionalProperty(VEILARB_KASSERING_IDENTER_PROPERTY).orElse("");

    public KasserController(AktivitetDAO aktivitetDAO, AuthService auth) {
        this.aktivitetDAO = aktivitetDAO;
        this.auth = auth;
    }

    @PUT
    @Path("/{aktivitetId}")
    public boolean kasserAktivitet(@PathParam("aktivitetId") String aktivitetId) {
        long id = Long.parseLong(aktivitetId);
        AktivitetData aktivitetData = aktivitetDAO.hentAktivitet(id);

        return kjorHvisTilgang(aktivitetData.getAktorId(), aktivitetId, () -> aktivitetDAO.kasserAktivitet(id));
    }

    private boolean kjorHvisTilgang(String aktorId, String id, Supplier<Boolean> fn) {

        auth.sjekkVeilederHarSkriveTilgangTilPerson(aktorId);

        String veilederIdent = SubjectHandler.getIdent().orElse(null);
        List<String> godkjente = Arrays.asList(godkjenteIdenter.split(","));
        if (!godkjente.contains(veilederIdent)) {
            log.error("[KASSERING] {} har ikke tilgang til kassering av {} aktivitet", veilederIdent, aktorId);
            throw new IngenTilgang(String.format("[KASSERING] %s har ikke tilgang til kassinger av %s aktivitet", veilederIdent, aktorId));
        }

        boolean updated = fn.get();

        log.info("[KASSERING] {} kasserte en aktivitet. AktoerId: {} aktivitet_id: {}", veilederIdent, aktorId, id);
        return updated;
    }
}
