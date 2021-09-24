package no.nav.veilarbaktivitet.stilling_fra_nav;

import io.micrometer.core.annotation.Timed;
import lombok.AllArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.base.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.base.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.base.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.person.InnsenderData;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.person.AuthService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@EnableScheduling
@AllArgsConstructor
public class DelingAvCvService {
    private final AktivitetDAO aktivitetDAO;
    private final DelingAvCvDAO delingAvCvDAO;
    private final AuthService authService;
    private final AktivitetService aktivitetService;
    private final StillingFraNavProducerClient stillingFraNavProducerClient;
    private final BrukernotifikasjonService brukernotifikasjonService;
    private final StillingFraNavMetrikker metrikker;

    public boolean aktivitetAlleredeOpprettetForBestillingsId(String bestillingsId) {
        return delingAvCvDAO.eksistererDelingAvCv(bestillingsId);
    }

    @Transactional
    public AktivitetData behandleSvarPaaOmCvSkalDeles(AktivitetData aktivitetData, boolean kanDeles, boolean erEksternBruker) {

        AktivitetData endeligAktivitet = oppdaterSvarPaaOmCvKanDeles(aktivitetData, kanDeles, erEksternBruker);

        brukernotifikasjonService.oppgaveDone(aktivitetData.getId(), VarselType.STILLING_FRA_NAV);
        stillingFraNavProducerClient.sendSvart(endeligAktivitet);
        metrikker.countSvar(erEksternBruker, kanDeles);

        return endeligAktivitet;
    }

    @Transactional
    @Timed(value = "avsluttUtloptStillingFraNavEn")
    public void avsluttAktivitet(AktivitetData aktivitet, Person person) {
        AktivitetData nyAktivitet = aktivitet.toBuilder()
                .status(AktivitetStatus.AVBRUTT)
                .avsluttetKommentar("Avsluttet fordi svarfrist har utløpt")
                .build();

        aktivitetService.oppdaterStatus(aktivitet, nyAktivitet, person);
        stillingFraNavProducerClient.sendSvarfristUtlopt(nyAktivitet);
    }

    public AktivitetData oppdaterSoknadsstatus(AktivitetData aktivitet, Soknadsstatus soknadsstatus) {
        Person innloggetBruker = authService.getLoggedInnUser().orElseThrow(RuntimeException::new);

        var nyStillingFraNavData = aktivitet.getStillingFraNavData().withSoknadsstatus(soknadsstatus);
        var nyAktivitet = aktivitet.toBuilder()
                .lagtInnAv(innloggetBruker.tilBrukerType())
                .stillingFraNavData(nyStillingFraNavData)
                .transaksjonsType(AktivitetTransaksjonsType.SOKNADSSTATUS_ENDRET)
                .endretAv(innloggetBruker.get())
                .build();

        return aktivitetDAO.oppdaterAktivitet(nyAktivitet);
    }

    private AktivitetData oppdaterSvarPaaOmCvKanDeles(AktivitetData aktivitetData, boolean kanDeles, boolean erEksternBruker) {
        Person innloggetBruker = authService.getLoggedInnUser().orElseThrow(RuntimeException::new);

        var deleCvDetaljer = CvKanDelesData.builder()
                .kanDeles(kanDeles)
                .endretTidspunkt(new Date())
                .endretAvType(erEksternBruker ? InnsenderData.BRUKER : InnsenderData.NAV)
                .endretAv(innloggetBruker.get())
                .build();


        var stillingFraNavData = aktivitetData.getStillingFraNavData().withCvKanDelesData(deleCvDetaljer);
        if (kanDeles) {
            stillingFraNavData = stillingFraNavData.withSoknadsstatus(Soknadsstatus.VENTER);
        }

        aktivitetService.svarPaaKanCvDeles(aktivitetData, aktivitetData.withStillingFraNavData(stillingFraNavData), innloggetBruker);
        var aktivitetMedCvSvar = aktivitetService.hentAktivitetMedForhaandsorientering(aktivitetData.getId());

        var statusOppdatering = aktivitetMedCvSvar.toBuilder();

        if (kanDeles) {
            statusOppdatering.status(AktivitetStatus.GJENNOMFORES);
        } else {
            statusOppdatering
                    .status(AktivitetStatus.AVBRUTT)
                    .avsluttetKommentar("Automatisk avsluttet fordi cv ikke skal deles");
        }

        return aktivitetService.oppdaterStatus(aktivitetMedCvSvar, statusOppdatering.build(), innloggetBruker);
    }
}
