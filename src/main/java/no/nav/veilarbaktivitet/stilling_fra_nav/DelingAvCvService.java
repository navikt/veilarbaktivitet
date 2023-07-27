package no.nav.veilarbaktivitet.stilling_fra_nav;

import io.micrometer.core.annotation.Timed;
import lombok.AllArgsConstructor;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.person.Innsender;
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
    private final IAuthService authService;
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
        AktivitetData nyAktivitet = aktivitetService.avsluttStillingFraNav(aktivitet, person.tilIdent());
        stillingFraNavProducerClient.sendSvarfristUtlopt(nyAktivitet);
        metrikker.countTidsfristUtlopt();
    }

    @Transactional
    @Timed(value = "stillingFraNavAvbruttEllerFullfortUtenSvar")
    public void notifiserAvbruttEllerFullfortUtenSvar(AktivitetData aktivitet) {
        var endretAv = Person.systemUser();
        AktivitetData nyAktivitet = aktivitet.toBuilder()
                .endretAv(endretAv.get())
                .endretAvType(endretAv.tilInnsenderType())
                .stillingFraNavData(aktivitet.getStillingFraNavData().withLivslopsStatus(LivslopsStatus.AVBRUTT_AV_BRUKER))
                .build();
        aktivitetService.oppdaterAktivitet(aktivitet, nyAktivitet);
        stillingFraNavProducerClient.sendAvbruttEllerFullfortUtenSvar(aktivitet);
        metrikker.countManuletAvbrutt(aktivitet.getEndretAvType());
    }

    public AktivitetData oppdaterSoknadsstatus(AktivitetData aktivitet, Soknadsstatus soknadsstatus, Person endretAv) {

        var nyStillingFraNavData = aktivitet.getStillingFraNavData().withSoknadsstatus(soknadsstatus);
        var nyAktivitet = aktivitet.toBuilder()
                .endretAvType(endretAv.tilInnsenderType())
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



    private AktivitetData oppdaterSvarPaaOmCvKanDeles(AktivitetData originalAktivitetData, boolean kanDeles, Date avtaltDato, boolean erEksternBruker) {
        Person innloggetBruker = Person.of(authService.getLoggedInnUser());

        var deleCvDetaljer = CvKanDelesData.builder()
                .kanDeles(kanDeles)
                .endretTidspunkt(new Date())
                .endretAvType(erEksternBruker ? Innsender.BRUKER : Innsender.NAV)
                .avtaltDato(avtaltDato)
                .endretAv(innloggetBruker.get())
                .build();


        var stillingFraNavData = originalAktivitetData.getStillingFraNavData()
                .withCvKanDelesData(deleCvDetaljer)
                .withLivslopsStatus(LivslopsStatus.HAR_SVART);
        if (kanDeles) {
            stillingFraNavData = stillingFraNavData.withSoknadsstatus(Soknadsstatus.VENTER);
        }
        var aktivitetData = originalAktivitetData
                .withEndretAv(innloggetBruker.get())
                .withEndretAvType(innloggetBruker.tilInnsenderType())
                .withStillingFraNavData(stillingFraNavData);
        aktivitetService.svarPaaKanCvDeles(originalAktivitetData, aktivitetData);
        var aktivitetMedCvSvar = aktivitetService.hentAktivitetMedForhaandsorientering(originalAktivitetData.getId());

        var aktivitetMedStatusOppdatering = aktivitetMedCvSvar.toBuilder();

        if (kanDeles) {
            aktivitetMedStatusOppdatering.status(AktivitetStatus.GJENNOMFORES);
        } else {
            aktivitetMedStatusOppdatering
                    .status(AktivitetStatus.AVBRUTT)
                    .avsluttetKommentar("Automatisk avsluttet fordi cv ikke skal deles");
        }

        return aktivitetService.oppdaterStatus(aktivitetMedCvSvar, aktivitetMedStatusOppdatering.build());
    }



}
