package no.nav.veilarbaktivitet.aktivitet;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import no.nav.veilarbaktivitet.aktivitet.domain.*;
import no.nav.veilarbaktivitet.avtalt_med_nav.AvtaltMedNavService;
import no.nav.veilarbaktivitet.avtalt_med_nav.Forhaandsorientering;
import no.nav.veilarbaktivitet.kvp.KvpService;
import no.nav.veilarbaktivitet.oppfolging.oppfolgingsperiode.OppfolgingsperiodeDao;
import no.nav.veilarbaktivitet.oppfolging.siste_periode.OppfolgingsperiodeService;
import no.nav.veilarbaktivitet.oppfolging.siste_periode.SistePeriodeDAO;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.stilling_fra_nav.LivslopsStatus;
import no.nav.veilarbaktivitet.util.MappingUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static no.nav.common.utils.StringUtils.nullOrEmpty;

@Service
@AllArgsConstructor
public class AktivitetService {

    private final AktivitetDAO aktivitetDAO;
    private final SistePeriodeDAO sistePeriodeDAO;
    private final AvtaltMedNavService avtaltMedNavService;
    private final KvpService kvpService;
    private final MetricService metricService;
    private final OppfolgingsperiodeService oppfolgingsperiodeService;

    public List<AktivitetData> hentAktiviteterForAktorId(Person.AktorId aktorId) {
        var aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(aktorId);
        var fhoIder = aktiviteter.stream().map(AktivitetData::getFhoId).collect(Collectors.toList());
        var forhaandsorienteringer = avtaltMedNavService.hentFHO(fhoIder);

        return aktiviteter.stream()
                .map(mergeMedForhaandsorientering(forhaandsorienteringer))
                .collect(Collectors.toList());
    }

    public AktivitetData hentAktivitetMedForhaandsorientering(long id) {
        var aktivitet = aktivitetDAO.hentAktivitet(id);
        var fho = avtaltMedNavService.hentFHO(aktivitet.getFhoId());

        return aktivitet
                .withForhaandsorientering(fho);
    }

    public AktivitetData hentAktivitetMedFHOForVersion(long version) {
        AktivitetData aktivitetData = aktivitetDAO.hentAktivitetVersion(version);
        Forhaandsorientering forhaandsorientering = avtaltMedNavService.hentFHO(aktivitetData.getFhoId());

        return aktivitetData.withForhaandsorientering(forhaandsorientering);
    }

    public List<AktivitetData> hentAktivitetVersjoner(long id) {
        return aktivitetDAO.hentAktivitetVersjoner(id);
    }

    public AktivitetData opprettAktivitet(Person.AktorId aktorId, AktivitetData aktivitet, Person endretAvPerson) {
        UUID oppfolgingsperiode = sistePeriodeDAO.hentSisteOppfolgingsPeriode(aktorId.get()).oppfolgingsperiode();
        // TODO sjekke om periode er aktiv?

        if (oppfolgingsperiode == null) {
            oppfolgingsperiode = oppfolgingsperiodeService.fallbackKallOppfolging(aktorId);
        }

        AktivitetData nyAktivivitet = aktivitet
                .toBuilder()
                .aktorId(aktorId.get())
                .lagtInnAv(endretAvPerson.tilBrukerType())
                .transaksjonsType(AktivitetTransaksjonsType.OPPRETTET)
                .opprettetDato(new Date())
                .endretAv(endretAvPerson.get())
                .automatiskOpprettet(aktivitet.isAutomatiskOpprettet())
                .oppfolgingsperiodeId(oppfolgingsperiode)
                .build();

        AktivitetData kvpAktivivitet = kvpService.tagUsingKVP(nyAktivivitet);
        ;
        nyAktivivitet = aktivitetDAO.opprettNyAktivitet(kvpAktivivitet);

        metricService.opprettNyAktivitetMetrikk(aktivitet);
        return nyAktivivitet;
    }

    @Transactional
    public AktivitetData oppdaterStatus(AktivitetData originalAktivitet, AktivitetData aktivitet, Person endretAv) {
        var nyAktivitet = originalAktivitet
                .toBuilder()
                .status(aktivitet.getStatus())
                .lagtInnAv(endretAv.tilBrukerType())
                .avsluttetKommentar(aktivitet.getAvsluttetKommentar())
                .transaksjonsType(AktivitetTransaksjonsType.STATUS_ENDRET)
                .endretAv(endretAv.get())
                .build();


        if(nyAktivitet.getStatus() == AktivitetStatus.AVBRUTT || nyAktivitet.getStatus() == AktivitetStatus.FULLFORT){
            avtaltMedNavService.settVarselFerdig(originalAktivitet.getFhoId());
        }
        return aktivitetDAO.oppdaterAktivitet(nyAktivitet);
    }

    public AktivitetData avsluttStillingFraNav(AktivitetData originalAktivitet, Person endretAv) {
        val originalStillingFraNav = originalAktivitet.getStillingFraNavData();
        val nyStillingFraNav = originalStillingFraNav.withLivslopsStatus(LivslopsStatus.AVBRUTT_AV_SYSTEM);

        val nyAktivitet = originalAktivitet
                .toBuilder()
                .endretAv(endretAv.get())
                .lagtInnAv(endretAv.tilBrukerType())
                .status(AktivitetStatus.AVBRUTT)
                .avsluttetKommentar("Avsluttet fordi svarfrist har utløpt")
                .transaksjonsType(AktivitetTransaksjonsType.STATUS_ENDRET)
                .stillingFraNavData(nyStillingFraNav)
                .build();

        return aktivitetDAO.oppdaterAktivitet(nyAktivitet);
    }

    public void oppdaterEtikett(AktivitetData originalAktivitet, AktivitetData aktivitet, Person endretAv) {
        val nyEtikett = aktivitet.getStillingsSoekAktivitetData().getStillingsoekEtikett();

        val originalStillingsAktivitet = originalAktivitet.getStillingsSoekAktivitetData();
        val nyStillingsAktivitet = originalStillingsAktivitet.withStillingsoekEtikett(nyEtikett);

        val nyAktivitet = originalAktivitet
                .toBuilder()
                .lagtInnAv(endretAv.tilBrukerType())
                .stillingsSoekAktivitetData(nyStillingsAktivitet)
                .transaksjonsType(AktivitetTransaksjonsType.ETIKETT_ENDRET)
                .endretAv(endretAv.get())
                .build();

        aktivitetDAO.oppdaterAktivitet(nyAktivitet);
    }

    public void oppdaterAktivitetFrist(AktivitetData originalAktivitet, AktivitetData aktivitetData, @NonNull Person endretAv) {
        val oppdatertAktivitetMedNyFrist = originalAktivitet
                .toBuilder()
                .lagtInnAv(endretAv.tilBrukerType())
                .transaksjonsType(AktivitetTransaksjonsType.AVTALT_DATO_ENDRET)
                .tilDato(aktivitetData.getTilDato())
                .endretAv(endretAv.get())
                .build();
        aktivitetDAO.oppdaterAktivitet(oppdatertAktivitetMedNyFrist);
    }

    public void oppdaterMoteTidStedOgKanal(AktivitetData originalAktivitet, AktivitetData aktivitetData, @NonNull Person endretAv) {
        val oppdatertAktivitetMedNyFrist = originalAktivitet
                .toBuilder()
                .lagtInnAv(endretAv.tilBrukerType())
                .transaksjonsType(AktivitetTransaksjonsType.MOTE_TID_OG_STED_ENDRET)
                .fraDato(aktivitetData.getFraDato())
                .tilDato(aktivitetData.getTilDato())
                .moteData(ofNullable(originalAktivitet.getMoteData()).map(moteData ->
                        moteData.withAdresse(aktivitetData.getMoteData().getAdresse())
                                .withKanal(aktivitetData.getMoteData().getKanal())
                ).orElse(null))
                .endretAv(endretAv.get())
                .build();
        aktivitetDAO.oppdaterAktivitet(oppdatertAktivitetMedNyFrist);
    }

    public void oppdaterReferat(
            AktivitetData originalAktivitet,
            AktivitetData aktivitetData,
            @NonNull Person endretAv
    ) {
        val transaksjon = getReferatTransakjsonType(originalAktivitet, aktivitetData);

        val merger = MappingUtils.merge(originalAktivitet, aktivitetData);
        aktivitetDAO.oppdaterAktivitet(originalAktivitet
                .withEndretAv(endretAv.get())
                .withLagtInnAv(endretAv.tilBrukerType())
                .withTransaksjonsType(transaksjon)
                .withMoteData(merger.map(AktivitetData::getMoteData).merge(this::mergeReferat))
        );
    }

    private AktivitetTransaksjonsType getReferatTransakjsonType(AktivitetData originalAktivitet,
                                                                AktivitetData aktivitetData) {
        val transaksjon = nullOrEmpty(originalAktivitet.getMoteData().getReferat())
                ? AktivitetTransaksjonsType.REFERAT_OPPRETTET : AktivitetTransaksjonsType.REFERAT_ENDRET;

        if (!originalAktivitet.getMoteData().isReferatPublisert() && aktivitetData.getMoteData().isReferatPublisert()) {
            return AktivitetTransaksjonsType.REFERAT_PUBLISERT;
        }
        return transaksjon;
    }

    private MoteData mergeReferat(MoteData originalMoteData, MoteData moteData) {
        return originalMoteData
                .withReferat(moteData.getReferat())
                .withReferatPublisert(moteData.isReferatPublisert());
    }

    public void svarPaaKanCvDeles(AktivitetData originalAktivitet, AktivitetData aktivitet, @NonNull Person endretAv) {
        aktivitetDAO.oppdaterAktivitet(originalAktivitet
                .withEndretAv(endretAv.get())
                .withLagtInnAv(endretAv.tilBrukerType())
                .withTransaksjonsType(AktivitetTransaksjonsType.DEL_CV_SVART)
                .withStillingFraNavData(aktivitet.getStillingFraNavData()));
    }

    public void oppdaterAktivitet(AktivitetData originalAktivitet, AktivitetData aktivitet, @NonNull Person endretAv) {
        val blittAvtalt = originalAktivitet.isAvtalt() != aktivitet.isAvtalt();
        val transType = blittAvtalt ? AktivitetTransaksjonsType.AVTALT : AktivitetTransaksjonsType.DETALJER_ENDRET;

        val merger = MappingUtils.merge(originalAktivitet, aktivitet);
        aktivitetDAO.oppdaterAktivitet(originalAktivitet
                .toBuilder()
                .fraDato(aktivitet.getFraDato())
                .tilDato(aktivitet.getTilDato())
                .tittel(aktivitet.getTittel())
                .beskrivelse(aktivitet.getBeskrivelse())
                .lagtInnAv(endretAv.tilBrukerType())
                .avsluttetKommentar(aktivitet.getAvsluttetKommentar())
                .lenke(aktivitet.getLenke())
                .transaksjonsType(transType)
                .versjon(aktivitet.getVersjon())
                .endretAv(endretAv.get())
                .avtalt(aktivitet.isAvtalt())
                .stillingsSoekAktivitetData(merger.map(AktivitetData::getStillingsSoekAktivitetData).merge(this::mergeStillingSok))
                .egenAktivitetData(merger.map(AktivitetData::getEgenAktivitetData).merge(this::mergeEgenAktivitetData))
                .sokeAvtaleAktivitetData(merger.map(AktivitetData::getSokeAvtaleAktivitetData).merge(this::mergeSokeAvtaleAktivitetData))
                .iJobbAktivitetData(merger.map(AktivitetData::getIJobbAktivitetData).merge(this::mergeIJobbAktivitetData))
                .behandlingAktivitetData(merger.map(AktivitetData::getBehandlingAktivitetData).merge(this::mergeBehandlingAktivitetData))
                .moteData(merger.map(AktivitetData::getMoteData).merge(this::mergeMoteData))
                .stillingFraNavData(aktivitet.getStillingFraNavData())
                .build()
        );
        metricService.oppdaterAktivitetMetrikk(aktivitet, blittAvtalt, originalAktivitet.isAutomatiskOpprettet());
    }

    private BehandlingAktivitetData mergeBehandlingAktivitetData(BehandlingAktivitetData originalBehandlingAktivitetData, BehandlingAktivitetData behandlingAktivitetData) {
        return originalBehandlingAktivitetData
                .withBehandlingType(behandlingAktivitetData.getBehandlingType())
                .withBehandlingSted(behandlingAktivitetData.getBehandlingSted())
                .withEffekt(behandlingAktivitetData.getEffekt())
                .withBehandlingOppfolging(behandlingAktivitetData.getBehandlingOppfolging());
    }

    private IJobbAktivitetData mergeIJobbAktivitetData(IJobbAktivitetData originalIJobbAktivitetData, IJobbAktivitetData iJobbAktivitetData) {
        return originalIJobbAktivitetData
                .withJobbStatusType(iJobbAktivitetData.getJobbStatusType())
                .withAnsettelsesforhold(iJobbAktivitetData.getAnsettelsesforhold())
                .withArbeidstid(iJobbAktivitetData.getArbeidstid());
    }

    private SokeAvtaleAktivitetData mergeSokeAvtaleAktivitetData(SokeAvtaleAktivitetData originalSokeAvtaleAktivitetData, SokeAvtaleAktivitetData sokeAvtaleAktivitetData) {
        return originalSokeAvtaleAktivitetData
                .withAntallStillingerSokes(sokeAvtaleAktivitetData.getAntallStillingerSokes())
                .withAntallStillingerIUken(sokeAvtaleAktivitetData.getAntallStillingerIUken())
                .withAvtaleOppfolging(sokeAvtaleAktivitetData.getAvtaleOppfolging());
    }

    private EgenAktivitetData mergeEgenAktivitetData(EgenAktivitetData originalEgenAktivitetData, EgenAktivitetData egenAktivitetData) {
        return originalEgenAktivitetData
                .withOppfolging(egenAktivitetData.getOppfolging())
                .withHensikt(egenAktivitetData.getHensikt());
    }

    private MoteData mergeMoteData(MoteData originalMoteData, MoteData moteData) {
        // Referat-felter settes gjennom egne operasjoner, se oppdaterReferat()
        return originalMoteData
                .withAdresse(moteData.getAdresse())
                .withForberedelser(moteData.getForberedelser())
                .withKanal(moteData.getKanal());
    }

    private StillingsoekAktivitetData mergeStillingSok(StillingsoekAktivitetData originalStillingsoekAktivitetData, StillingsoekAktivitetData stillingsoekAktivitetData) {
        return originalStillingsoekAktivitetData
                .withArbeidsgiver(stillingsoekAktivitetData.getArbeidsgiver())
                .withArbeidssted(stillingsoekAktivitetData.getArbeidssted())
                .withKontaktPerson(stillingsoekAktivitetData.getKontaktPerson())
                .withStillingsTittel(stillingsoekAktivitetData.getStillingsTittel());
    }

    @Transactional
    public void settAktiviteterTilHistoriske(Person.AktorId aktoerId, Date sluttDato) {
        hentAktiviteterForAktorId(aktoerId)
                .stream()
                .filter(a -> skalBliHistorisk(a, sluttDato))
                .map(a -> a.withTransaksjonsType(AktivitetTransaksjonsType.BLE_HISTORISK).withHistoriskDato(sluttDato))
                .forEach(a -> {
                    avtaltMedNavService.settVarselFerdig(a.getFhoId());
                    aktivitetDAO.oppdaterAktivitet(a);
                });
    }

    private boolean skalBliHistorisk(AktivitetData aktivitetData, Date sluttdato) {
        return aktivitetData.getHistoriskDato() == null && aktivitetData.getOpprettetDato().before(sluttdato);
    }

    @Transactional
    public AktivitetData settLestAvBrukerTidspunkt(Long aktivitetId) {
        aktivitetDAO.insertLestAvBrukerTidspunkt(aktivitetId);
        return hentAktivitetMedForhaandsorientering(aktivitetId);
    }

    private Function<AktivitetData, AktivitetData> mergeMedForhaandsorientering(List<Forhaandsorientering> forhaandsorienteringData) {
        return aktivitetData -> aktivitetData.withForhaandsorientering(forhaandsorienteringData
                .stream()
                .filter(fhoData -> fhoData.getId().equals(aktivitetData.getFhoId()))
                .findAny()
                .orElse(null)
        );
    }

}
