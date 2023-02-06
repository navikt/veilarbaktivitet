package no.nav.veilarbaktivitet.aktivitet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

import static no.nav.common.utils.EnvironmentUtils.getOptionalProperty;
import static no.nav.veilarbaktivitet.config.ApplicationContext.VEILARB_KASSERING_IDENTER_PROPERTY;

@Slf4j
@Component
@RequestMapping("/api/kassering")
@RequiredArgsConstructor
public class KasserController {

    private final IAuthService authService;
    private final AktivitetDAO aktivitetDAO;

    private final KasseringDAO kasseringDAO;

    private final String godkjenteIdenter = getOptionalProperty(VEILARB_KASSERING_IDENTER_PROPERTY).orElse("");

    @PutMapping("/{aktivitetId}")
    @ResponseStatus(value = HttpStatus.OK)
    public void kasserAktivitet(@PathVariable("aktivitetId") String aktivitetId) {
        long id = Long.parseLong(aktivitetId);
        AktivitetData aktivitetData = aktivitetDAO.hentAktivitet(id);

        kjorHvisTilgang(Person.aktorId(aktivitetData.getAktorId()), aktivitetId, () -> kasseringDAO.kasserAktivitet(id));
    }

    private boolean kjorHvisTilgang(Person.AktorId aktorId, String id, BooleanSupplier fn) {
        authService.sjekkInternbrukerHarSkriveTilgangTilPerson(aktorId.otherAktorId());

        var veilederIdent = authService.getInnloggetVeilederIdent();
        List<String> godkjente = Arrays.asList(godkjenteIdenter.split(","));

        if (!godkjente.contains(veilederIdent.get())) {
            log.error("[KASSERING] {} har ikke tilgang til kassering av {} aktivitet", veilederIdent, aktorId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, String.format("[KASSERING] %s har ikke tilgang til kassinger av %s aktivitet", veilederIdent, aktorId));
        }

        boolean updated = fn.getAsBoolean();

        log.info("[KASSERING] {} kasserte en aktivitet. AktoerId: {} aktivitet_id: {}", veilederIdent, aktorId, id);
        return updated;
    }
}
