package no.nav.veilarbaktivitet.aktivitet;

import lombok.AllArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.domain.*;
import no.nav.veilarbaktivitet.avtalt_med_nav.AvtaltMedNavService;
import no.nav.veilarbaktivitet.avtalt_med_nav.Forhaandsorientering;
import no.nav.veilarbaktivitet.oppfolging.periode.IngenGjeldendePeriodeException;
import no.nav.veilarbaktivitet.oppfolging.periode.SistePeriodeService;
import no.nav.veilarbaktivitet.oversikten.OversiktenService;
import no.nav.veilarbaktivitet.person.Innsender;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.stilling_fra_nav.CvKanDelesData;
import no.nav.veilarbaktivitet.stilling_fra_nav.LivslopsStatus;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData;
import no.nav.veilarbaktivitet.util.DateUtils;
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

@Service
@AllArgsConstructor
public class AktivitetService {

    private final AktivitetDAO aktivitetDAO;
    private final AvtaltMedNavService avtaltMedNavService;
    private final MetricService metricService;
    private final SistePeriodeService sistePeriodeService;
    private final OversiktenService oversiktenService;

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

    private AktivitetData enforceOppfolgingsPeriode(AktivitetData aktivitet, Person.AktorId aktorId) throws IngenGjeldendePeriodeException {
        if (aktivitet.getOppfolgingsperiodeId() == null) {
            var oppfolgingsperiode = sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(aktorId);
            return aktivitet.withOppfolgingsperiodeId(oppfolgingsperiode);
        } else {
            return aktivitet;
        }
    }

    public AktivitetData opprettAktivitet(AktivitetData aktivitet) throws IngenGjeldendePeriodeException {
        var nyAktivivitet = enforceOppfolgingsPeriode(aktivitet, aktivitet.getAktorId())
                .toBuilder()
                .transaksjonsType(AktivitetTransaksjonsType.OPPRETTET)
                .build();
        var opprettetAktivitet = aktivitetDAO.opprettNyAktivitet(nyAktivivitet);
        metricService.opprettNyAktivitetMetrikk(opprettetAktivitet);
        return opprettetAktivitet;
    }

    @Transactional
    public AktivitetData oppdaterStatus(AktivitetData originalAktivitet, AktivitetData aktivitet) {
        var nyAktivitet = originalAktivitet
                .toBuilder()
                .status(aktivitet.getStatus())
                .endretAv(aktivitet.getEndretAv())
                .endretAvType(aktivitet.getEndretAvType())
                .endretDato(aktivitet.getEndretDato())
                .avsluttetKommentar(aktivitet.getAvsluttetKommentar())
                .transaksjonsType(AktivitetTransaksjonsType.STATUS_ENDRET)
                .build();

        if(nyAktivitet.getStatus() == AktivitetStatus.AVBRUTT || nyAktivitet.getStatus() == AktivitetStatus.FULLFORT){
            avtaltMedNavService.settVarselFerdig(originalAktivitet.getFhoId());
        }

        if(erAvbruttMoteEllerSamtalereferat(nyAktivitet)){
            oversiktenService.lagreStoppMeldingOmUdeltSamtalereferatIUtboks(nyAktivitet.getAktorId(), nyAktivitet.getId());
        }
        return aktivitetDAO.oppdaterAktivitet(nyAktivitet);
    }

    private boolean erAvbruttMoteEllerSamtalereferat(AktivitetData aktivitet){
        return (aktivitet.getAktivitetType() == AktivitetTypeData.MOTE ||
                aktivitet.getAktivitetType() == AktivitetTypeData.SAMTALEREFERAT) &&
                aktivitet.getStatus() == AktivitetStatus.AVBRUTT;
    }

    public AktivitetData avsluttStillingFraNav(AktivitetData originalAktivitet, Ident endretAv) {
        final var originalStillingFraNav = originalAktivitet.getStillingFraNavData();
        final var nyStillingFraNav = originalStillingFraNav.withLivslopsStatus(LivslopsStatus.AVBRUTT_AV_SYSTEM);

        final var nyAktivitet = originalAktivitet
                .toBuilder()
                .endretAv(endretAv.ident())
                .endretAvType(endretAv.identType().toInnsender())
                .endretDato(new Date())
                .status(AktivitetStatus.AVBRUTT)
                .avsluttetKommentar("Avsluttet fordi svarfrist har utløpt")
                .transaksjonsType(AktivitetTransaksjonsType.STATUS_ENDRET)
                .stillingFraNavData(nyStillingFraNav)
                .build();

        return aktivitetDAO.oppdaterAktivitet(nyAktivitet);
    }

    public void oppdaterEtikett(AktivitetData originalAktivitet, AktivitetData aktivitet) {
        final var nyEtikett = aktivitet.getStillingsSoekAktivitetData().getStillingsoekEtikett();
        final var originalStillingsAktivitet = originalAktivitet.getStillingsSoekAktivitetData();
        final var nyStillingsAktivitet = originalStillingsAktivitet.withStillingsoekEtikett(nyEtikett);
        final var nyAktivitet = originalAktivitet
                .toBuilder()
                .endretAvType(aktivitet.getEndretAvType())
                .endretAv(aktivitet.getEndretAv())
                .endretDato(aktivitet.getEndretDato())
                .stillingsSoekAktivitetData(nyStillingsAktivitet)
                .transaksjonsType(AktivitetTransaksjonsType.ETIKETT_ENDRET)
                .build();
        aktivitetDAO.oppdaterAktivitet(nyAktivitet);
    }

    public void oppdaterAktivitetFrist(AktivitetData originalAktivitet, AktivitetData aktivitetData) {
        final var oppdatertAktivitetMedNyFrist = originalAktivitet
                .toBuilder()
                .endretAvType(aktivitetData.getEndretAvType())
                .endretAv(aktivitetData.getEndretAv())
                .endretDato(aktivitetData.getEndretDato())
                .transaksjonsType(AktivitetTransaksjonsType.AVTALT_DATO_ENDRET)
                .tilDato(aktivitetData.getTilDato())
                .build();
        aktivitetDAO.oppdaterAktivitet(oppdatertAktivitetMedNyFrist);
    }

    public void oppdaterMoteTidStedOgKanal(AktivitetData originalAktivitet, AktivitetData aktivitetData) {
        final var oppdatertAktivitetMedNyFrist = originalAktivitet
                .toBuilder()
                .endretAvType(aktivitetData.getEndretAvType())
                .endretAv(aktivitetData.getEndretAv())
                .endretDato(aktivitetData.getEndretDato())
                .transaksjonsType(AktivitetTransaksjonsType.MOTE_TID_OG_STED_ENDRET)
                .fraDato(aktivitetData.getFraDato())
                .tilDato(aktivitetData.getTilDato())
                .moteData(ofNullable(originalAktivitet.getMoteData()).map(moteData ->
                        moteData.withAdresse(aktivitetData.getMoteData().getAdresse())
                                .withKanal(aktivitetData.getMoteData().getKanal())
                ).orElse(null))
                .build();
        aktivitetDAO.oppdaterAktivitet(oppdatertAktivitetMedNyFrist);
    }

    public AktivitetData oppdaterReferat(
            AktivitetData originalAktivitet,
            AktivitetData aktivitetData
    ) {
        final var transaksjon = getReferatTransakjsonType(originalAktivitet, aktivitetData);

        final var merger = MappingUtils.merge(originalAktivitet, aktivitetData);
        return aktivitetDAO.oppdaterAktivitet(originalAktivitet
                .withEndretDato(aktivitetData.getEndretDato())
                .withEndretAv(aktivitetData.getEndretAv())
                .withEndretAvType(Innsender.NAV) // Bare NAV kan endre referat
                .withTransaksjonsType(transaksjon)
                .withMoteData(merger.map(AktivitetData::getMoteData).merge(this::mergeReferat))
        );
    }

    private AktivitetTransaksjonsType getReferatTransakjsonType(AktivitetData originalAktivitet,
                                                                AktivitetData aktivitetData) {
        final var transaksjon = nullOrEmpty(originalAktivitet.getMoteData().getReferat())
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

    public void svarPaaKanCvDeles(AktivitetData originalAktivitet, AktivitetData aktivitet) {
        aktivitetDAO.oppdaterAktivitet(originalAktivitet
                .withEndretAv(aktivitet.getEndretAv())
                .withEndretAvType(aktivitet.getEndretAvType())
                .withEndretDato(new Date())
                .withTransaksjonsType(AktivitetTransaksjonsType.DEL_CV_SVART)
                .withStillingFraNavData(aktivitet.getStillingFraNavData()));
    }

    public AktivitetData oppdaterAktivitet(AktivitetData originalAktivitet, AktivitetData aktivitet) {
        final var blittAvtalt = originalAktivitet.isAvtalt() != aktivitet.isAvtalt();
        if (blittAvtalt) {
            throw new IllegalArgumentException(String.format("Kan ikke sette avtalt for aktivitetsid: %s gjennom oppdaterAktivitet", originalAktivitet.getId()));
        }
        final var transType = AktivitetTransaksjonsType.DETALJER_ENDRET;
        final var merger = MappingUtils.merge(originalAktivitet, aktivitet);
        final var result = aktivitetDAO.oppdaterAktivitet(originalAktivitet
                .toBuilder()
                .avsluttetKommentar(aktivitet.getAvsluttetKommentar())
                .behandlingAktivitetData(merger.map(AktivitetData::getBehandlingAktivitetData).merge(this::mergeBehandlingAktivitetData))
                .beskrivelse(aktivitet.getBeskrivelse())
                .egenAktivitetData(merger.map(AktivitetData::getEgenAktivitetData).merge(this::mergeEgenAktivitetData))
                .endretAv(aktivitet.getEndretAv())
                .endretDato(aktivitet.getEndretDato())
                .fraDato(aktivitet.getFraDato())
                .iJobbAktivitetData(merger.map(AktivitetData::getIJobbAktivitetData).merge(this::mergeIJobbAktivitetData))
                .endretAvType(aktivitet.getEndretAvType())
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
                .build());
        metricService.oppdaterAktivitetMetrikk(aktivitet, blittAvtalt, originalAktivitet.isAutomatiskOpprettet());
        return result;
    }

    public AktivitetData settAvtalt(AktivitetData originalAktivitet, Ident endretAv, LocalDateTime endretTidspunkt) {
        if (!originalAktivitet.endringTillatt()) throw new IllegalStateException(String.format("Ikke lov å endre aktivtet med id %s", originalAktivitet.getId()));
        return aktivitetDAO.oppdaterAktivitet(
                originalAktivitet
                        .withEndretDato(DateUtils.localDateTimeToDate(endretTidspunkt))
                        .withAvtalt(true)
                        .withTransaksjonsType(AktivitetTransaksjonsType.AVTALT)
                        .withEndretAv(endretAv.ident())
                        .withEndretAvType(endretAv.identType().toInnsender())
                );
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

    private EksternAktivitetData mergeEksternAktivitet(EksternAktivitetData original, EksternAktivitetData newData) {
        return original.copy(
                    newData.getSource(),
                    newData.getTiltaksKode(),
                    original.getOpprettetSomHistorisk(),
                    original.getOppfolgingsperiodeSlutt(),
                    newData.getArenaId(),
                    newData.getType(),
                    newData.getOppgave(),
                    newData.getHandlinger(),
                    newData.getDetaljer(),
                    newData.getEtiketter(),
                    newData.getEndretTidspunktKilde()
                );
    }

    @Transactional
    public void settAktiviteterTilHistoriske(UUID oppfolgingsperiodeId, ZonedDateTime sluttDato) {
        Date sluttDatoDate = new Date(sluttDato.toInstant().toEpochMilli());
        aktivitetDAO.hentAktiviteterForOppfolgingsperiodeId(oppfolgingsperiodeId)
                .stream()
                .filter(it -> it.getHistoriskDato() == null)
                .map(aktivitet -> aktivitet
                        .withTransaksjonsType(AktivitetTransaksjonsType.BLE_HISTORISK)
                        .withHistoriskDato(sluttDatoDate)
                        .withEndretDato(new Date())
                        .withEndretAvType(Innsender.SYSTEM)
                        .withEndretAv("veilarbaktivitet")
                )
                .forEach(aktivitet -> {
                    avtaltMedNavService.settVarselFerdig(aktivitet.getFhoId());
                    aktivitetDAO.oppdaterAktivitet(aktivitet);
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
