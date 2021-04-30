package no.nav.veilarbaktivitet.service;

import lombok.AllArgsConstructor;
import lombok.val;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.*;
import no.nav.veilarbaktivitet.kvp.KvpService;
import no.nav.veilarbaktivitet.util.MappingUtils;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static no.nav.common.utils.StringUtils.nullOrEmpty;

@Service
@AllArgsConstructor
public class AktivitetService {

    private final AktivitetDAO aktivitetDAO;
    private final KvpService kvpService;
    private final MetricService metricService;

    public List<AktivitetData> hentAktiviteterForAktorId(Person.AktorId aktorId) {
        return aktivitetDAO.hentAktiviteterForAktorId(aktorId);
    }

    public AktivitetData hentAktivitet(long id) {
        return aktivitetDAO.hentAktivitet(id);
    }

    public List<AktivitetData> hentAktivitetVersjoner(long id) {
        return aktivitetDAO.hentAktivitetVersjoner(id);
    }

    public long opprettAktivitet(Person.AktorId aktorId, AktivitetData aktivitet, Person endretAvPerson) {
        String endretAv = Optional.ofNullable(endretAvPerson).map(Person::get).orElse(null);
        long aktivitetId = aktivitetDAO.getNextUniqueAktivitetId();
        AktivitetData nyAktivivitet = aktivitet
                .toBuilder()
                .id(aktivitetId)
                .aktorId(aktorId.get())
                .lagtInnAv(aktivitet.getLagtInnAv())
                .transaksjonsType(AktivitetTransaksjonsType.OPPRETTET)
                .opprettetDato(new Date())
                .endretAv(endretAv)
                .automatiskOpprettet(aktivitet.isAutomatiskOpprettet())
                .build();

        AktivitetData kvpAktivivitet = kvpService.tagUsingKVP(nyAktivivitet);
        aktivitetDAO.insertAktivitet(kvpAktivivitet);

        metricService.opprettNyAktivitetMetrikk(aktivitet);
        return aktivitetId;
    }

    public void oppdaterStatus(AktivitetData originalAktivitet, AktivitetData aktivitet, Person endretAv) {
        val nyAktivitet = originalAktivitet
                .toBuilder()
                .status(aktivitet.getStatus())
                .lagtInnAv(aktivitet.getLagtInnAv())
                .avsluttetKommentar(aktivitet.getAvsluttetKommentar())
                .transaksjonsType(AktivitetTransaksjonsType.STATUS_ENDRET)
                .endretAv(endretAv != null ? endretAv.get() : null)
                .build();

        aktivitetDAO.insertAktivitet(nyAktivitet);
    }

    public void oppdaterEtikett(AktivitetData originalAktivitet, AktivitetData aktivitet, Person endretAv) {
        val nyEtikett = aktivitet.getStillingsSoekAktivitetData().getStillingsoekEtikett();

        val originalStillingsAktivitet = originalAktivitet.getStillingsSoekAktivitetData();
        val nyStillingsAktivitet = originalStillingsAktivitet.withStillingsoekEtikett(nyEtikett);

        val nyAktivitet = originalAktivitet
                .toBuilder()
                .lagtInnAv(aktivitet.getLagtInnAv())
                .stillingsSoekAktivitetData(nyStillingsAktivitet)
                .transaksjonsType(AktivitetTransaksjonsType.ETIKETT_ENDRET)
                .endretAv(endretAv != null ? endretAv.get() : null)
                .build();

        aktivitetDAO.insertAktivitet(nyAktivitet);
    }

    public void oppdaterAktivitetFrist(AktivitetData originalAktivitet, AktivitetData aktivitetData, Person endretAv) {
        val oppdatertAktivitetMedNyFrist = originalAktivitet
                .toBuilder()
                .lagtInnAv(aktivitetData.getLagtInnAv())
                .transaksjonsType(AktivitetTransaksjonsType.AVTALT_DATO_ENDRET)
                .tilDato(aktivitetData.getTilDato())
                .endretAv(endretAv != null ? endretAv.get() : null)
                .build();
        aktivitetDAO.insertAktivitet(oppdatertAktivitetMedNyFrist);
    }

    public void oppdaterMoteTidStedOgKanal(AktivitetData originalAktivitet, AktivitetData aktivitetData, Person endretAv) {
        val oppdatertAktivitetMedNyFrist = originalAktivitet
                .toBuilder()
                .lagtInnAv(aktivitetData.getLagtInnAv())
                .transaksjonsType(AktivitetTransaksjonsType.MOTE_TID_OG_STED_ENDRET)
                .fraDato(aktivitetData.getFraDato())
                .tilDato(aktivitetData.getTilDato())
                .moteData(ofNullable(originalAktivitet.getMoteData()).map(moteData ->
                        moteData.withAdresse(aktivitetData.getMoteData().getAdresse())
                                .withKanal(aktivitetData.getMoteData().getKanal())
                ).orElse(null))
                .endretAv(endretAv != null ? endretAv.get() : null)
                .build();
        aktivitetDAO.insertAktivitet(oppdatertAktivitetMedNyFrist);
    }

    public void oppdaterReferat(
            AktivitetData originalAktivitet,
            AktivitetData aktivitetData,
            Person endretAv
    ) {
        val transaksjon = getReferatTransakjsonType(originalAktivitet, aktivitetData);

        val merger = MappingUtils.merge(originalAktivitet, aktivitetData);
        aktivitetDAO.insertAktivitet(originalAktivitet
                .withEndretAv(endretAv.get())
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

    public void oppdaterAktivitet(AktivitetData originalAktivitet, AktivitetData aktivitet, Person endretAv) {
        val blittAvtalt = originalAktivitet.isAvtalt() != aktivitet.isAvtalt();
        val transType = blittAvtalt ? AktivitetTransaksjonsType.AVTALT : AktivitetTransaksjonsType.DETALJER_ENDRET;

        val merger = MappingUtils.merge(originalAktivitet, aktivitet);
        aktivitetDAO.insertAktivitet(originalAktivitet
                .toBuilder()
                .fraDato(aktivitet.getFraDato())
                .tilDato(aktivitet.getTilDato())
                .tittel(aktivitet.getTittel())
                .beskrivelse(aktivitet.getBeskrivelse())
                .lagtInnAv(aktivitet.getLagtInnAv())
                .avsluttetKommentar(aktivitet.getAvsluttetKommentar())
                .lenke(aktivitet.getLenke())
                .transaksjonsType(transType)
                .versjon(aktivitet.getVersjon())
                .endretAv(endretAv != null ? endretAv.get() : null)
                .avtalt(aktivitet.isAvtalt())
                .stillingsSoekAktivitetData(merger.map(AktivitetData::getStillingsSoekAktivitetData).merge(this::mergeStillingSok))
                .egenAktivitetData(merger.map(AktivitetData::getEgenAktivitetData).merge(this::mergeEgenAktivitetData))
                .sokeAvtaleAktivitetData(merger.map(AktivitetData::getSokeAvtaleAktivitetData).merge(this::mergeSokeAvtaleAktivitetData))
                .iJobbAktivitetData(merger.map(AktivitetData::getIJobbAktivitetData).merge(this::mergeIJobbAktivitetData))
                .behandlingAktivitetData(merger.map(AktivitetData::getBehandlingAktivitetData).merge(this::mergeBehandlingAktivitetData))
                .moteData(merger.map(AktivitetData::getMoteData).merge(this::mergeMoteData))
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

    public void settAktiviteterTilHistoriske(Person.AktorId aktoerId, Date sluttDato) {
        hentAktiviteterForAktorId(aktoerId)
                .stream()
                .filter(a -> skalBliHistorisk(a, sluttDato))
                .map(a -> a.withTransaksjonsType(AktivitetTransaksjonsType.BLE_HISTORISK).withHistoriskDato(sluttDato))
                .forEach(aktivitetDAO::insertAktivitet);
    }

    private boolean skalBliHistorisk(AktivitetData aktivitetData, Date sluttdato) {
        return aktivitetData.getHistoriskDato() == null && aktivitetData.getOpprettetDato().before(sluttdato);
    }

    @Transactional
    public AktivitetData settLestAvBrukerTidspunkt(Long aktivitetId) {
        aktivitetDAO.insertLestAvBrukerTidspunkt(aktivitetId);
        return hentAktivitet(aktivitetId);
    }

}
