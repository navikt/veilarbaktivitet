package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.service.AktivitetAppService;
import no.nav.veilarbaktivitet.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stillingFraNav")
public class StillingFraNavController {
    private final AuthService authService;
    private final AktivitetAppService aktivitetAppService;
    private final DelingAvCvService service;

    @PutMapping("/kanDeleCV")
    public AktivitetDTO oppdaterKanCvDeles(@RequestParam long aktivitetId, @RequestBody DelingAvCvDTO delingAvCvDTO) {
        boolean erEksternBruker = authService.erEksternBruker();
        var aktivitet = aktivitetAppService
                .hentAktivitet(aktivitetId);

        if (aktivitet.getAktivitetType() != AktivitetTypeData.STILLING_FRA_NAV) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Kan bare dele cv på aktiviteter med type %s", AktivitetTypeData.STILLING_FRA_NAV));
        }

        if (aktivitet.getVersjon() != delingAvCvDTO.aktivitetVersjon) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("Kan ikke dele cv på aktivitetversjon: %s når siste versjon er: %s", delingAvCvDTO.aktivitetVersjon, aktivitet.getVersjon()));

        }

        return Optional.of(aktivitet)
                .map(a -> service.behandleSvarPaaOmCvSkalDeles(a, delingAvCvDTO.kanDeles, erEksternBruker))
                .map(a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
    }

    @PutMapping("/soknadStatus")
    public AktivitetDTO oppdaterSoknadstatus(@RequestParam long aktivitetId, @RequestBody StatusDTO statusDTO) {

    }
}
