package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.AllArgsConstructor;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.domain.InnsenderData;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.service.AktivitetAppService;
import no.nav.veilarbaktivitet.service.AktivitetService;
import no.nav.veilarbaktivitet.service.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@AllArgsConstructor
public class DelingAvCvService {
    private final DelingAvCvDAO delingAvCvDAO;
    private final AuthService authService;
    private final AktivitetService aktivitetService;

    public boolean aktivitetAlleredeOpprettetForBestillingsId(String bestillingsId) {
        return delingAvCvDAO.eksistererDelingAvCv(bestillingsId);
    }

    @Transactional
    public AktivitetData oppdaterSvarPaaOmCvSkalDeles(AktivitetData aktivitetData, boolean kanDeles, boolean erEksternBruker) {
        Person innloggetBruker = authService.getLoggedInnUser().orElseThrow(RuntimeException::new);

        var deleCvDetaljer = CvKanDelesData.builder()
                .kanDeles(kanDeles)
                .endretTidspunkt(new Date())
                .endretAvType(erEksternBruker? InnsenderData.BRUKER : InnsenderData.NAV)
                .endretAv(innloggetBruker.get())
                .build();

        var stillingFraNavData = aktivitetData.getStillingFraNavData().withCvKanDelesData(deleCvDetaljer);

        aktivitetService.oppdaterAktivitet(aktivitetData, aktivitetData.withStillingFraNavData(stillingFraNavData), innloggetBruker);
        var aktivitetMedCvSvar = aktivitetService.hentAktivitetMedForhaandsorientering(aktivitetData.getId());

        if (kanDeles) {
            return aktivitetService.oppdaterStatus(aktivitetMedCvSvar, aktivitetMedCvSvar.withStatus(AktivitetStatus.GJENNOMFORES), innloggetBruker);
        }
        else {
            var nyAktivitet = aktivitetMedCvSvar
                    .withStatus(AktivitetStatus.AVBRUTT)
                    .withAvsluttetKommentar("Automatisk avsluttet fordi cv ikke skal deles");

            return aktivitetService.oppdaterStatus(aktivitetMedCvSvar, nyAktivitet, innloggetBruker);
        }

    }


}
