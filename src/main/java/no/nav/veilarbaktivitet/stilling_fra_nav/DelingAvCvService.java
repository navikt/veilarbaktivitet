package no.nav.veilarbaktivitet.stilling_fra_nav;

import io.micrometer.core.annotation.Timed;
import lombok.AllArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.person.AuthService;
import no.nav.veilarbaktivitet.person.InnsenderData;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.Arbeidssted;
import no.nav.veilarbaktivitet.util.TekstformatteringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

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
    public AktivitetData behandleSvarPaaOmCvSkalDeles(AktivitetData aktivitetData, boolean kanDeles, Date avtaltDato, boolean erEksternBruker) {

        AktivitetData endeligAktivitet = oppdaterSvarPaaOmCvKanDeles(aktivitetData, kanDeles, avtaltDato, erEksternBruker);

        brukernotifikasjonService.setDone(aktivitetData.getId(), VarselType.STILLING_FRA_NAV);
        stillingFraNavProducerClient.sendSvart(endeligAktivitet);
        metrikker.countSvar(erEksternBruker, kanDeles);

        return endeligAktivitet;
    }

    @Transactional
    @Timed(value = "avsluttUtloptStillingFraNavEn")
    public void avsluttAktivitet(AktivitetData aktivitet, Person person) {
        AktivitetData nyAktivitet = aktivitetService.avsluttStillingFraNav(aktivitet, person);
        stillingFraNavProducerClient.sendSvarfristUtlopt(nyAktivitet);
        metrikker.countTidsfristUtlopt();
    }

    @Transactional
    @Timed(value = "stillingFraNavAvbruttEllerFullfortUtenSvar")
    public void notifiserAvbruttEllerFullfortUtenSvar(AktivitetData aktivitet, Person person) {
        AktivitetData nyAktivitet = aktivitet.toBuilder()
                .stillingFraNavData(aktivitet.getStillingFraNavData().withLivslopsStatus(LivslopsStatus.AVBRUTT_AV_BRUKER))
                .build();
        aktivitetService.oppdaterAktivitet(aktivitet, nyAktivitet, person);
        stillingFraNavProducerClient.sendAvbruttEllerFullfortUtenSvar(aktivitet);
        metrikker.countManuletAvbrutt(aktivitet.getLagtInnAv());
    }

    public AktivitetData oppdaterSoknadsstatus(AktivitetData aktivitet, Soknadsstatus soknadsstatus, Person endretAv) {

        var nyStillingFraNavData = aktivitet.getStillingFraNavData().withSoknadsstatus(soknadsstatus);
        var nyAktivitet = aktivitet.toBuilder()
                .lagtInnAv(endretAv.tilBrukerType())
                .stillingFraNavData(nyStillingFraNavData)
                .transaksjonsType(AktivitetTransaksjonsType.SOKNADSSTATUS_ENDRET)
                .endretAv(endretAv.get())
                .build();

        return aktivitetDAO.oppdaterAktivitet(nyAktivitet);
    }

    @NotNull
    public static String utledArbeidstedtekst(List<Arbeidssted> arbeidssteder) {

        Predicate<Arbeidssted> harKommune = arbeidssted ->
                arbeidssted.getKommune() != null
                && !arbeidssted.getKommune().isEmpty();

        Predicate<Arbeidssted> harLand = arbeidssted ->
                arbeidssted.getLand() != null
                && !arbeidssted.getLand().isEmpty();

        Predicate<Arbeidssted> harFylke = arbeidssted ->
                arbeidssted.getFylke() != null
                && !arbeidssted.getFylke().isEmpty();

        Predicate<Arbeidssted> harLandSattTilNorge = arbeidssted ->
                "Norge".equalsIgnoreCase(arbeidssted.getLand());

        Predicate<Arbeidssted> harKommuneINorge = harKommune.and(
                harLandSattTilNorge.or(not(harLand))
        );

        Predicate<Arbeidssted> harFylkeINorge = harFylke.and(
                harLandSattTilNorge.or(not(harLand))
        );

        Function<Arbeidssted, String> velgKommuneFylkeEllerLand = arbeidssted -> {
            if (harKommuneINorge.test(arbeidssted)) return arbeidssted.getKommune();
            else if (harFylkeINorge.test(arbeidssted)) return arbeidssted.getFylke();
            else return arbeidssted.getLand();
        };

        return arbeidssteder.stream()
                .filter(harLand.or(harKommuneINorge).or(harFylkeINorge))
                .map(velgKommuneFylkeEllerLand.andThen(TekstformatteringUtils::storeForbokstaverStedsnavn))
                .collect(Collectors.joining(", "));
    }



    private AktivitetData oppdaterSvarPaaOmCvKanDeles(AktivitetData aktivitetData, boolean kanDeles, Date avtaltDato, boolean erEksternBruker) {
        Person innloggetBruker = authService.getLoggedInnUser().orElseThrow(RuntimeException::new);

        var deleCvDetaljer = CvKanDelesData.builder()
                .kanDeles(kanDeles)
                .endretTidspunkt(new Date())
                .endretAvType(erEksternBruker ? InnsenderData.BRUKER : InnsenderData.NAV)
                .avtaltDato(avtaltDato)
                .endretAv(innloggetBruker.get())
                .build();


        var stillingFraNavData = aktivitetData.getStillingFraNavData()
                .withCvKanDelesData(deleCvDetaljer)
                .withLivslopsStatus(LivslopsStatus.HAR_SVART);
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
