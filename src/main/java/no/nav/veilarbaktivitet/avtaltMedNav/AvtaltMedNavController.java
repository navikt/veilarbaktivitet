package no.nav.veilarbaktivitet.avtaltMedNav;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.service.AuthService;
import no.nav.veilarbaktivitet.service.MetricService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;

@Transactional
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/avtaltMedNav")
public class AvtaltMedNavController {

    private final MetricService metricService;
    private final AktivitetDAO aktivitetDAO;
    private final AuthService authService;

    @PutMapping
    public AktivitetDTO markerSomAvtaltMedNav(@RequestBody AvtaltMedNav avtaltMedNav, @RequestParam long aktivitetId) {
        AktivitetData aktivitet = aktivitetDAO.hentAktivitet(aktivitetId);
        Forhaandsorientering forhaandsorientering = avtaltMedNav.getForhaandsorientering();

        if (aktivitet == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aktiviteten eksisterer ikke");
        }

        authService.sjekkTilgangOgInternBruker(aktivitet.getAktorId(), aktivitet.getKontorsperreEnhetId());
        validerInput(avtaltMedNav, aktivitet, forhaandsorientering);

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

        metricService.oppdaterAktivitetMetrikk(aktivitet, true, aktivitet.isAutomatiskOpprettet());

        return AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetDAO.hentAktivitet(aktivitetId));

    }

    @PutMapping("/lest")
    public AktivitetDTO lest(@RequestBody LestDTO lestDTO) {

        if (lestDTO.aktivitetId == null || lestDTO.aktivitetVersion == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "aktivitetId og aktivitetVersion må vere satt");
        }

        AktivitetData aktivitetData = aktivitetDAO.hentAktivitet(lestDTO.aktivitetId);

        authService.sjekkTilgangTilPerson(Person.aktorId(aktivitetData.getAktorId()));

        if (aktivitetData.getForhaandsorientering() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fho eksister ikke");
        }
        if (aktivitetData.getForhaandsorientering().getLest() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Allerede lest");
        }

        Forhaandsorientering fho = aktivitetData.getForhaandsorientering().toBuilder().lest(new Date()).build();

        AktivitetData aktivitet = aktivitetData.toBuilder().forhaandsorientering(fho).build();

        aktivitetDAO.insertAktivitet(aktivitet);

        return AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetDAO.hentAktivitet(lestDTO.aktivitetId));
    }

    private void validerInput(AvtaltMedNav avtaltMedNav, AktivitetData aktivitet, Forhaandsorientering forhaandsorientering) {
        if (avtaltMedNav.getAktivitetVersjon() != aktivitet.getVersjon()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Feil aktivitetversjon");
        }

        if (forhaandsorientering == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "forhaandsorientering kan ikke være null");
        }

        if (forhaandsorientering.getType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "forhaandsorientering.type kan ikke være null");
        }

        if (aktivitet.isAvtalt()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aktiviteten er allerede avtalt med NAV");
        }
    }

}
