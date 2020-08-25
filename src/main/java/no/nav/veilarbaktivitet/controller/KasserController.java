package no.nav.veilarbaktivitet.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.types.feil.IngenTilgang;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static no.nav.common.utils.EnvironmentUtils.getOptionalProperty;
import static no.nav.veilarbaktivitet.config.ApplicationContext.VEILARB_KASSERING_IDENTER_PROPERTY;

@Slf4j
@Component
@RequestMapping("/api/kassering")
public class KasserController {

    private final AktivitetDAO aktivitetDAO;
    private final AuthService auth;

    private final String godkjenteIdenter = getOptionalProperty(VEILARB_KASSERING_IDENTER_PROPERTY).orElse("");

    public KasserController(AktivitetDAO aktivitetDAO, AuthService auth) {
        this.aktivitetDAO = aktivitetDAO;
        this.auth = auth;
    }

    @PutMapping("/{aktivitetId}")
    @ResponseStatus(value = HttpStatus.OK)
    public void kasserAktivitet(@PathVariable("aktivitetId") String aktivitetId) {
        long id = Long.parseLong(aktivitetId);
        AktivitetData aktivitetData = aktivitetDAO.hentAktivitet(id);

        kjorHvisTilgang(aktivitetData.getAktorId(), aktivitetId, () -> aktivitetDAO.kasserAktivitet(id));
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
