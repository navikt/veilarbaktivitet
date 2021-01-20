package no.nav.veilarbaktivitet.avtaltMedNav;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.service.AktivitetAppService;
import no.nav.veilarbaktivitet.service.FunksjonelleMetrikker;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Transactional
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/avtaltMedNav")
public class AvtaltMedNavController {

    private final AktivitetAppService service;
    private final FunksjonelleMetrikker funksjonelleMetrikker;
    private final AktivitetDAO aktivitetDAO;


    @PutMapping
    public AktivitetDTO markerSomAvtaltMedNav(@RequestBody AvtaltMedNav avtaltMedNav, @RequestParam long aktivitetId) {
        AktivitetData aktivitet = service.hentAktivitet(aktivitetId);
        Forhaandsorientering forhaandsorientering = avtaltMedNav.getForhaandsorientering();

        if (aktivitet == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aktiviteten eksisterer ikke");
        }

        if (avtaltMedNav.getAktivitetVersjon() != aktivitet.getVersjon()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Feil aktivitetversjon");
        }

        if (forhaandsorientering.getType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "forhaandsorientering.type kan ikke være null");
        }

        if (aktivitet.isAvtalt()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aktiviteten er allerede avtalt med NAV");
        }

        if (forhaandsorientering.getTekst() != null && forhaandsorientering.getTekst().isEmpty()) {
            forhaandsorientering.setTekst(null);
        }

        AktivitetData nyAktivitet = aktivitet
                .toBuilder()
                .avtalt(true)
                .forhaandsorientering(forhaandsorientering)
                .transaksjonsType(AktivitetTransaksjonsType.AVTALT)
                .build();

        aktivitetDAO.insertAktivitet(nyAktivitet);

        funksjonelleMetrikker.oppdaterAktivitetMetrikk(aktivitet, true, aktivitet.isAutomatiskOpprettet());

        return AktivitetDTOMapper.mapTilAktivitetDTO(service.hentAktivitet(aktivitetId));

    }
}
