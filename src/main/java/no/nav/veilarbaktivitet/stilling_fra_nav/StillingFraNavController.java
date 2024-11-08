package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.RequiredArgsConstructor;
import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.poao.dab.spring_auth.TilgangsType;
import no.nav.veilarbaktivitet.aktivitet.AktivitetAppService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.config.AktivitetResource;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stillingFraNav")
public class StillingFraNavController {
    private final IAuthService authService;
    private final AktivitetAppService aktivitetAppService;
    private final DelingAvCvService service;

    @PutMapping("/kanDeleCV")
    @AuthorizeFnr(auditlogMessage = "oppdater kan dele CV", resourceIdParamName = "aktivitetId", resourceType = AktivitetResource.class, tilgangsType = TilgangsType.SKRIVE)
    public AktivitetDTO oppdaterKanCvDeles(@RequestParam("aktivitetId") String aktivitetId, @RequestBody DelingAvCvDTO delingAvCvDTO) {
        boolean erEksternBruker = authService.erEksternBruker();
        var aktivitet = aktivitetAppService.hentAktivitet(Long.parseLong(aktivitetId));
        if (aktivitet.getAktivitetType() != AktivitetTypeData.STILLING_FRA_NAV) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Kan bare dele cv på aktiviteter med type %s", AktivitetTypeData.STILLING_FRA_NAV));
        }
        if (aktivitet.getStillingFraNavData().getSvarfrist().toInstant().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Svarfrist %s er utløpt.", aktivitet.getStillingFraNavData().getSvarfrist()));
        }
        if (aktivitet.getVersjon() != delingAvCvDTO.aktivitetVersjon) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("Kan ikke dele cv på aktivitetversjon: %s når siste versjon er: %s", delingAvCvDTO.aktivitetVersjon, aktivitet.getVersjon()));
        }
        if (!erEksternBruker && delingAvCvDTO.getAvtaltDato() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "avtaltDato er påkrevd når veileder svarer");
        }
        return Optional.of(aktivitet)
                .map(a -> service.behandleSvarPaaOmCvSkalDeles(a, delingAvCvDTO.kanDeles, delingAvCvDTO.avtaltDato, erEksternBruker))
                .map(a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
    }

    @PutMapping("/soknadStatus")
    @AuthorizeFnr(auditlogMessage = "oppdater soknadStatus", resourceIdParamName = "aktivitetId", resourceType = AktivitetResource.class, tilgangsType = TilgangsType.SKRIVE)
    public AktivitetDTO oppdaterSoknadstatus(@RequestParam("aktivitetId") String aktivitetId, @RequestBody SoknadsstatusDTO soknadsstatusDTO) {
        boolean erEksternBruker = authService.erEksternBruker();
        var aktivitet = aktivitetAppService.hentAktivitet(Long.parseLong(aktivitetId));
        authService.sjekkTilgangTilPerson(aktivitet.getAktorId().eksternBrukerId(), TilgangsType.SKRIVE);
        kanEndreAktivitetSoknadsstatusGuard(aktivitet, soknadsstatusDTO.getAktivitetVersjon());
        return Optional.of(aktivitet)
                .map(a -> service.oppdaterSoknadsstatus(a, soknadsstatusDTO.getSoknadsstatus(), Person.of(authService.getLoggedInnUser())))
                .map(a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker))
                .orElseThrow(RuntimeException::new);
    }

    private void kanEndreAktivitetSoknadsstatusGuard(AktivitetData orginalAktivitet, Long aktivitetVersjon) {
        if (!Objects.equals(orginalAktivitet.getVersjon(), aktivitetVersjon)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        } else if (orginalAktivitet.getHistoriskDato() != null) {
            // Søknadsstatus skal kunne endres selv om aktivitet er fullført eller avbrutt
            throw new IllegalArgumentException(
                    String.format("Kan ikke endre søknadsstatus på historisk aktivitet [%s]",
                            orginalAktivitet.getId())
            );
        }
    }
}
