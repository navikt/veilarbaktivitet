package no.nav.veilarbaktivitet.avtalt_med_nav;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.EnhetId;
import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.poao.dab.spring_auth.TilgangsType;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.config.AktivitetResource;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Transactional
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/avtaltMedNav")
public class AvtaltMedNavController {

    private final IAuthService authService;
    private final AvtaltMedNavService avtaltMedNavService;

    @PutMapping
    @AuthorizeFnr(auditlogMessage = "Opprett forhaandsorientering", resourceIdParamName = "aktivitetId", resourceType = AktivitetResource.class)
    public AktivitetDTO opprettFHO(@RequestBody AvtaltMedNavDTO avtaltMedNavDTO, @RequestParam String aktivitetId) {
        if (!authService.erInternBruker()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bare interne brukere kan opprettte FHO");
        var forhaandsorientering = avtaltMedNavDTO.getForhaandsorientering();

        if (forhaandsorientering == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "forhaandsorientering kan ikke være null");
        }
        if (forhaandsorientering.getType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "forhaandsorientering.type kan ikke være null");
        }

        AktivitetData aktivitet = avtaltMedNavService.hentAktivitet(Long.parseLong(aktivitetId));
        if (aktivitet == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aktiviteten eksisterer ikke");
        if (aktivitet.getKontorsperreEnhetId() != null) {
            authService.sjekkTilgangTilEnhet(EnhetId.of(aktivitet.getKontorsperreEnhetId()));
        }

        if (avtaltMedNavDTO.getAktivitetVersjon() != aktivitet.getVersjon()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Feil aktivitetversjon");
        }

        if (aktivitet.getFhoId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aktiviteten har allerede en forhåndsorientering");
        }

        return avtaltMedNavService.opprettFHO(avtaltMedNavDTO, Long.parseLong(aktivitetId), aktivitet.getAktorId(), authService.getInnloggetVeilederIdent());

    }

    @PutMapping("/lest")
    public AktivitetDTO lest(@RequestBody LestDTO lestDTO) {
        if (!authService.erEksternBruker()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bare eksterne kan lese en FHO");
        if (lestDTO.aktivitetId == null || lestDTO.aktivitetVersion == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "aktivitetId og aktivitetVersion må vere satt");
        }
        Forhaandsorientering fho = avtaltMedNavService.hentFhoForAktivitet(lestDTO.aktivitetId);
        if (fho == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FHO på aktivitet eksisterer ikke");
        if (fho.getLestDato() != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Allerede lest");
        authService.sjekkTilgangTilPerson(fho.getAktorId(), TilgangsType.LESE); //TODO SKRIVE
        Person innloggetBruker = Person.of(authService.getLoggedInnUser());
        return avtaltMedNavService.markerSomLest(fho, innloggetBruker, lestDTO.aktivitetVersion);
    }

}
