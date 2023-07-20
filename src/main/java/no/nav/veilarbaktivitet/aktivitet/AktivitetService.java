package no.nav.veilarbaktivitet.aktivitet;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import no.nav.veilarbaktivitet.aktivitet.domain.*;
import no.nav.veilarbaktivitet.avtalt_med_nav.AvtaltMedNavService;
import no.nav.veilarbaktivitet.avtalt_med_nav.Forhaandsorientering;
import no.nav.veilarbaktivitet.kvp.KvpService;
import no.nav.veilarbaktivitet.oppfolging.siste_periode.IngenGjeldendePeriodeException;
import no.nav.veilarbaktivitet.oppfolging.siste_periode.SistePeriodeService;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.stilling_fra_nav.CvKanDelesData;
import no.nav.veilarbaktivitet.stilling_fra_nav.LivslopsStatus;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData;
import no.nav.veilarbaktivitet.util.MappingUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static no.nav.common.utils.StringUtils.nullOrEmpty;
import static no.nav.veilarbaktivitet.util.DateUtils.localDateTimeToDate;

@Service
@AllArgsConstructor
public class AktivitetService {

    private final AktivitetDAO aktivitetDAO;
    private final AvtaltMedNavService avtaltMedNavService;
    private final KvpService kvpService;
    private final MetricService metricService;
    private final SistePeriodeService sistePeriodeService;

    public List<AktivitetData> hentAktiviteterForAktorId(Person.AktorId aktorId) {
        var aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(aktorId);
        var fhoIder = aktiviteter.stream().map(AktivitetData::getFhoId).toList();
        var forhaandsorienteringer = avtaltMedNavService.hentFHO(fhoIder);

        return aktiviteter.stream()
                .map(mergeMedForhaandsorientering(forhaandsorienteringer))
                .toList();
    }

    public AktivitetData hentAktivitetMedForhaandsorientering(long id) {
        var aktivitet = aktivitetDAO.hentAktivitet(id);
        var fho = avtaltMedNavService.hentFHO(aktivitet.getFhoId());

        return aktivitet
                .withForhaandsorientering(fho)
                .withFhoId(fho == null ? null : fho.getId());
    }

    public AktivitetData hentAktivitetMedFHOForVersion(long version) {
        AktivitetData aktivitetData = aktivitetDAO.hentAktivitetVersion(version);
        Forhaandsorientering forhaandsorientering = avtaltMedNavService.hentFHO(aktivitetData.getFhoId());

        return aktivitetData.withForhaandsorientering(forhaandsorientering);
    }

    public List<AktivitetData> hentAktivitetVersjoner(long id) {
        return aktivitetDAO.hentAktivitetVersjoner(id);
    }

    public AktivitetData opprettAktivitet(Person.AktorId aktorId, AktivitetData aktivitet, Ident endretAv) {
        return opprettAktivitet(aktorId, aktivitet, endretAv, LocalDateTime.now());
    }

    public AktivitetData opprettAktivitet(Person.AktorId aktorId, AktivitetData aktivitet, Ident endretAv, LocalDateTime opprettet) throws IngenGjeldendePeriodeException {
        UUID oppfolgingsperiode = sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(aktorId);
        return opprettAktivitet(aktorId, aktivitet, endretAv, opprettet, oppfolgingsperiode);
    }

    public AktivitetData opprettAktivitet(Person.AktorId aktorId, AktivitetData aktivitet, Ident endretAv, LocalDateTime opprettet, UUID oppfolgingsperiode) {

        AktivitetData nyAktivivitet = aktivitet
                .toBuilder()
                .aktorId(aktorId.get())
                .avtalt(aktivitet.isAvtalt())
                .endretAvType(endretAv.identType().toInnsender())
                .transaksjonsType(AktivitetTransaksjonsType.OPPRETTET)
                .opprettetDato(localDateTimeToDate(opprettet))
                .endretAv(endretAv.ident())
                .automatiskOpprettet(aktivitet.isAutomatiskOpprettet())
                .oppfolgingsperiodeId(oppfolgingsperiode)
                .build();

        AktivitetData kvpAktivivitet = kvpService.tagUsingKVP(nyAktivivitet);

        nyAktivivitet = aktivitetDAO.opprettNyAktivitet(kvpAktivivitet, opprettet);

        metricService.opprettNyAktivitetMetrikk(aktivitet);
        return nyAktivivitet;
    }

    @Transactional
    public AktivitetData oppdaterStatus(AktivitetData originalAktivitet, AktivitetData aktivitet, Ident endretAv) {
        return oppdaterStatus(originalAktivitet, aktivitet, endretAv, LocalDateTime.now());
    }

    @Transactional
    public AktivitetData oppdaterStatus(AktivitetData originalAktivitet, AktivitetData aktivitet, Ident endretAv, LocalDateTime endretDato) {
        var nyAktivitet = originalAktivitet
                .toBuilder()
                .status(aktivitet.getStatus())
                .endretAv(endretAv.ident())
                .endretAvType(endretAv.identType().toInnsender())
                .avsluttetKommentar(aktivitet.getAvsluttetKommentar())
                .transaksjonsType(AktivitetTransaksjonsType.STATUS_ENDRET)
                .build();


        if(nyAktivitet.getStatus() == AktivitetStatus.AVBRUTT || nyAktivitet.getStatus() == AktivitetStatus.FULLFORT){
            avtaltMedNavService.settVarselFerdig(originalAktivitet.getFhoId());
        }
        return aktivitetDAO.oppdaterAktivitet(nyAktivitet, endretDato);
    }

    public AktivitetData avsluttStillingFraNav(AktivitetData originalAktivitet, Person endretAv) {
        val originalStillingFraNav = originalAktivitet.getStillingFraNavData();
        val nyStillingFraNav = originalStillingFraNav.withLivslopsStatus(LivslopsStatus.AVBRUTT_AV_SYSTEM);

        val nyAktivitet = originalAktivitet
                .toBuilder()
                .endretAv(endretAv.get())
                .endretAvType(endretAv.tilInnsenderType())
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
                .endretAvType(endretAv.tilInnsenderType())
                .stillingsSoekAktivitetData(nyStillingsAktivitet)
                .transaksjonsType(AktivitetTransaksjonsType.ETIKETT_ENDRET)
                .endretAv(endretAv.get())
                .build();

        aktivitetDAO.oppdaterAktivitet(nyAktivitet);
    }

    public void oppdaterAktivitetFrist(AktivitetData originalAktivitet, AktivitetData aktivitetData, @NonNull Person endretAv) {
        val oppdatertAktivitetMedNyFrist = originalAktivitet
                .toBuilder()
                .endretAvType(endretAv.tilInnsenderType())
                .transaksjonsType(AktivitetTransaksjonsType.AVTALT_DATO_ENDRET)
                .tilDato(aktivitetData.getTilDato())
                .endretAv(endretAv.get())
                .build();
        aktivitetDAO.oppdaterAktivitet(oppdatertAktivitetMedNyFrist);
    }

    public void oppdaterMoteTidStedOgKanal(AktivitetData originalAktivitet, AktivitetData aktivitetData, @NonNull Person endretAv) {
        val oppdatertAktivitetMedNyFrist = originalAktivitet
                .toBuilder()
                .endretAvType(endretAv.tilInnsenderType())
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
                .withEndretAvType(endretAv.tilInnsenderType())
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
                .withEndretAvType(endretAv.tilInnsenderType())
                .withTransaksjonsType(AktivitetTransaksjonsType.DEL_CV_SVART)
                .withStillingFraNavData(aktivitet.getStillingFraNavData()));
    }

    // TODO i fremtiden: hvis endretDato ligger i "aktivitet", bruk det, hvis ikke, LocalDateTime.now() i DAO
    public AktivitetData oppdaterAktivitet(AktivitetData originalAktivitet, AktivitetData aktivitet, @NonNull Person endretAv, LocalDateTime endretDato) {
        val blittAvtalt = originalAktivitet.isAvtalt() != aktivitet.isAvtalt();
        if (blittAvtalt) {
            throw new IllegalArgumentException(String.format("Kan ikke sette avtalt for aktivitetsid: %s gjennom oppdaterAktivitet", originalAktivitet.getId()));
        }
        val transType = AktivitetTransaksjonsType.DETALJER_ENDRET;
        val merger = MappingUtils.merge(originalAktivitet, aktivitet);
        val result = aktivitetDAO.oppdaterAktivitet(originalAktivitet
                .toBuilder()
                .avsluttetKommentar(aktivitet.getAvsluttetKommentar())
                .behandlingAktivitetData(merger.map(AktivitetData::getBehandlingAktivitetData).merge(this::mergeBehandlingAktivitetData))
                .beskrivelse(aktivitet.getBeskrivelse())
                .egenAktivitetData(merger.map(AktivitetData::getEgenAktivitetData).merge(this::mergeEgenAktivitetData))
                .endretAv(endretAv.get())
                .fraDato(aktivitet.getFraDato())
                .iJobbAktivitetData(merger.map(AktivitetData::getIJobbAktivitetData).merge(this::mergeIJobbAktivitetData))
                .endretAvType(endretAv.tilInnsenderType())
                .lenke(aktivitet.getLenke())
                .moteData(merger.map(AktivitetData::getMoteData).merge(this::mergeMoteData))
                .sokeAvtaleAktivitetData(merger.map(AktivitetData::getSokeAvtaleAktivitetData).merge(this::mergeSokeAvtaleAktivitetData))
                .stillingFraNavData(merger.map(AktivitetData::getStillingFraNavData).merge(this::mergeStillingFraNav))
                .stillingsSoekAktivitetData(merger.map(AktivitetData::getStillingsSoekAktivitetData).merge(this::mergeStillingSok))
                .tilDato(aktivitet.getTilDato())
                .tittel(aktivitet.getTittel())
                .transaksjonsType(transType)
                .eksternAktivitetData(merger.map(AktivitetData::getEksternAktivitetData).merge(this::mergeEksternAktivitet))
                .fhoId(originalAktivitet.getFhoId() != null ? originalAktivitet.getFhoId() : aktivitet.getFhoId()) // Tilltater ikke å endre fhoId her
                .build(),
                endretDato);
        metricService.oppdaterAktivitetMetrikk(aktivitet, blittAvtalt, originalAktivitet.isAutomatiskOpprettet());
        return result;
    }

    public AktivitetData settAvtalt(AktivitetData originalAktivitet, Ident endretAv, LocalDateTime endretTidspunkt) {
        if (!originalAktivitet.endringTillatt()) throw new IllegalStateException(String.format("Ikke lov å endre aktivtet med id %s", originalAktivitet.getId()));
        return aktivitetDAO.oppdaterAktivitet(
                originalAktivitet
                        .withAvtalt(true)
                        .withTransaksjonsType(AktivitetTransaksjonsType.AVTALT)
                        .withEndretAv(endretAv.ident())
                        .withEndretAvType(endretAv.identType().toInnsender())
                        ,
                endretTidspunkt
                );
    }

    public AktivitetData oppdaterAktivitet(AktivitetData originalAktivitet, AktivitetData aktivitet, @NonNull Person endretAv) {
        return oppdaterAktivitet(originalAktivitet, aktivitet, endretAv, LocalDateTime.now());
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

    private EksternAktivitetData mergeEksternAktivitet(EksternAktivitetData left, EksternAktivitetData right) {
        return left.copy(
                    right.getSource(),
                    right.getTiltaksKode(),
                    left.getOpprettetSomHistorisk(),
                    left.getOppfolgingsperiodeSlutt(),
                    left.getArenaId(),
                    right.getType(),
                    right.getOppgave(),
                    right.getHandlinger(),
                    right.getDetaljer(),
                    right.getEtiketter()
                );
    }

    @Transactional
    public void settAktiviteterTilHistoriske(UUID oppfolingsperiode, ZonedDateTime sluttDato) {
        Date sluttDatoDate = new Date(sluttDato.toInstant().toEpochMilli());
        aktivitetDAO.hentAktiviteterForOppfolgingsperiodeId(oppfolingsperiode)
                .stream()
                .map(a -> a.withTransaksjonsType(AktivitetTransaksjonsType.BLE_HISTORISK).withHistoriskDato(sluttDatoDate))
                .forEach(a -> {
                    avtaltMedNavService.settVarselFerdig(a.getFhoId());
                    aktivitetDAO.oppdaterAktivitet(a);
                });
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


    private CvKanDelesData mergeCVKanDelesData(CvKanDelesData existing, CvKanDelesData updated) {
        return existing
                .withKanDeles(updated.getKanDeles())
                .withEndretAv(updated.getEndretAv())
                .withAvtaltDato(updated.getAvtaltDato())
                .withEndretAvType(updated.getEndretAvType())
                .withEndretTidspunkt(new Date());
    }
    private StillingFraNavData mergeStillingFraNav(StillingFraNavData existing, StillingFraNavData updated) {
        return existing
            .withArbeidssted(updated.getArbeidssted())
            .withArbeidsgiver(updated.getArbeidsgiver())
            .withCvKanDelesData(
                MappingUtils.merge(existing.getCvKanDelesData(), updated.getCvKanDelesData())
                    .merge(this::mergeCVKanDelesData)
            )
            .withLivslopsStatus(updated.getLivslopsStatus())
            .withSoknadsstatus(updated.getSoknadsstatus())
            .withBestillingsId(updated.getBestillingsId())
            .withSvarfrist(updated.getSvarfrist())
            .withKontaktpersonData(updated.getKontaktpersonData())
            .withDetaljer(updated.getDetaljer())
            .withVarselId(updated.getVarselId());
    }

}
