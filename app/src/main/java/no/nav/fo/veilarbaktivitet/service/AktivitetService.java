package no.nav.fo.veilarbaktivitet.service;

import lombok.val;
import no.nav.apiapp.feil.VersjonsKonflikt;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.fo.veilarbaktivitet.domain.*;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static java.util.Optional.ofNullable;
import static no.nav.apiapp.util.ObjectUtils.notEqual;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.AVBRUTT;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.FULLFORT;
import static no.nav.fo.veilarbaktivitet.util.MappingUtils.merge;

@Component
public class AktivitetService {
    private final AktivitetDAO aktivitetDAO;

    @Inject
    public AktivitetService(AktivitetDAO aktivitetDAO) {
        this.aktivitetDAO = aktivitetDAO;
    }

    public List<AktivitetData> hentAktiviteterForAktorId(String aktorId) {
        return aktivitetDAO.hentAktiviteterForAktorId(aktorId);
    }

    public AktivitetData hentAktivitet(long id) {
        return aktivitetDAO.hentAktivitet(id);
    }


    public List<AktivitetData> hentAktivitetVersjoner(long id) {
        return aktivitetDAO.hentAktivitetVersjoner(id);
    }

    public long opprettAktivitet(String aktorId, AktivitetData aktivitet, String endretAv) {
        val aktivitetId = aktivitetDAO.getNextUniqueAktivitetId();
        val nyAktivivitet = aktivitet
                .toBuilder()
                .id(aktivitetId)
                .aktorId(aktorId)
                .lagtInnAv(aktivitet.getLagtInnAv())
                .transaksjonsType(AktivitetTransaksjonsType.OPPRETTET)
                .opprettetDato(new Date())
                .endretAv(endretAv)
                .build();

        aktivitetDAO.insertAktivitet(nyAktivivitet);
        return aktivitetId;
    }


    public void oppdaterStatus(AktivitetData aktivitet, String endretAv) {
        val orginalAktivitet = aktivitetDAO.hentAktivitet(aktivitet.getId());
        kanEndreAktivitetGuard(orginalAktivitet, aktivitet);

        val nyAktivitet = orginalAktivitet
                .toBuilder()
                .status(aktivitet.getStatus())
                .lagtInnAv(aktivitet.getLagtInnAv())
                .avsluttetKommentar(aktivitet.getAvsluttetKommentar())
                .transaksjonsType(AktivitetTransaksjonsType.STATUS_ENDRET)
                .endretAv(endretAv)
                .build();

        insertAktivitet(nyAktivitet);
    }

    public void oppdaterEtikett(AktivitetData aktivitet, String endretAv) {
        val orginalAktivitet = aktivitetDAO.hentAktivitet(aktivitet.getId());
        kanEndreAktivitetGuard(orginalAktivitet, aktivitet);

        val nyEtikett = aktivitet.getStillingsSoekAktivitetData().getStillingsoekEtikett();

        val orginalStillingsAktivitet = orginalAktivitet.getStillingsSoekAktivitetData();
        val nyStillingsAktivitet = orginalStillingsAktivitet.withStillingsoekEtikett(nyEtikett);

        val nyAktivitet = orginalAktivitet
                .toBuilder()
                .lagtInnAv(aktivitet.getLagtInnAv())
                .stillingsSoekAktivitetData(nyStillingsAktivitet)
                .transaksjonsType(AktivitetTransaksjonsType.ETIKETT_ENDRET)
                .endretAv(endretAv)
                .build();

        insertAktivitet(nyAktivitet);
    }

    public void slettAktivitet(long aktivitetId) {
        aktivitetDAO.slettAktivitet(aktivitetId);
    }

    public void oppdaterAktivitetFrist(AktivitetData orginalAktivitet, AktivitetData aktivitetData, String endretAv) {
        kanEndreAktivitetGuard(orginalAktivitet, aktivitetData);
        val oppdatertAktivitetMedNyFrist = orginalAktivitet
                .toBuilder()
                .lagtInnAv(aktivitetData.getLagtInnAv())
                .transaksjonsType(AktivitetTransaksjonsType.AVTALT_DATO_ENDRET)
                .tilDato(aktivitetData.getTilDato())
                .endretAv(endretAv)
                .build();
        insertAktivitet(oppdatertAktivitetMedNyFrist);
    }

    public void oppdaterMoteTidOgSted(AktivitetData orginalAktivitet, AktivitetData aktivitetData, String endretAv) {
        kanEndreAktivitetGuard(orginalAktivitet, aktivitetData);
        val oppdatertAktivitetMedNyFrist = orginalAktivitet
                .toBuilder()
                .lagtInnAv(aktivitetData.getLagtInnAv())
                .transaksjonsType(AktivitetTransaksjonsType.MOTE_TID_OG_STED_ENDRET )
                .fraDato(aktivitetData.getFraDato())
                .tilDato(aktivitetData.getTilDato())
                .moteData(ofNullable(orginalAktivitet.getMoteData()).map(d -> d.withAdresse(aktivitetData.getMoteData().getAdresse())).orElse(null))
                .endretAv(endretAv)
                .build();
        insertAktivitet(oppdatertAktivitetMedNyFrist);
    }

    public void oppdaterReferat(
            AktivitetData aktivitet,
            AktivitetTransaksjonsType aktivitetTransaksjonsType,
            String endretAv
    ) {
        val orginalAktivitet = aktivitetDAO.hentAktivitet(aktivitet.getId());
        kanEndreAktivitetGuard(orginalAktivitet, aktivitet);

        val merger = merge(orginalAktivitet, aktivitet);
        insertAktivitet(orginalAktivitet
                .withEndretAv(endretAv)
                .withTransaksjonsType(aktivitetTransaksjonsType)
                .withMoteData(merger.map(AktivitetData::getMoteData).merge(this::mergeReferat))
        );
    }

    private MoteData mergeReferat(MoteData orginalMoteData, MoteData moteData) {
        return orginalMoteData
                .withReferat(moteData.getReferat())
                .withReferatPublisert(moteData.isReferatPublisert());
    }

    public void oppdaterAktivitet(AktivitetData orginalAktivitet, AktivitetData aktivitet, String endretAv) {
        kanEndreAktivitetGuard(orginalAktivitet, aktivitet);

        val blittAvtalt = orginalAktivitet.isAvtalt() != aktivitet.isAvtalt();
        val transType = blittAvtalt ? AktivitetTransaksjonsType.AVTALT : AktivitetTransaksjonsType.DETALJER_ENDRET;

        val merger = merge(orginalAktivitet, aktivitet);
        insertAktivitet(orginalAktivitet
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
                .endretAv(endretAv)
                .avtalt(aktivitet.isAvtalt())
                .stillingsSoekAktivitetData(merger.map(AktivitetData::getStillingsSoekAktivitetData).merge(this::mergeStillingSok))
                .egenAktivitetData(merger.map(AktivitetData::getEgenAktivitetData).merge(this::mergeEgenAktivitetData))
                .sokeAvtaleAktivitetData(merger.map(AktivitetData::getSokeAvtaleAktivitetData).merge(this::mergeSokeAvtaleAktivitetData))
                .iJobbAktivitetData(merger.map(AktivitetData::getIJobbAktivitetData).merge(this::mergeIJobbAktivitetData))
                .behandlingAktivitetData(merger.map(AktivitetData::getBehandlingAktivitetData).merge(this::mergeBehandlingAktivitetData))
                .moteData(merger.map(AktivitetData::getMoteData).merge(this::mergeMoteData))
                .build()
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
                .withAvtaleOppfolging(sokeAvtaleAktivitetData.getAvtaleOppfolging());
    }

    private EgenAktivitetData mergeEgenAktivitetData(EgenAktivitetData orginalEgenAktivitetData, EgenAktivitetData egenAktivitetData) {
        return orginalEgenAktivitetData
                .withOppfolging(egenAktivitetData.getOppfolging())
                .withHensikt(egenAktivitetData.getHensikt());
    }

    private MoteData mergeMoteData(MoteData orginalMoteData, MoteData moteData) {
        // Referat-felter settes gjennom egne operasjoner, se oppdaterReferat()
        return orginalMoteData
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

    private void insertAktivitet(AktivitetData aktivitetData) {
        try {
            aktivitetDAO.insertAktivitet(aktivitetData);
        } catch (DuplicateKeyException e) {
            throw new VersjonsKonflikt();
        }
    }

    private void kanEndreAktivitetGuard(AktivitetData orginalAktivitet, AktivitetData aktivitet) {
        if (notEqual(orginalAktivitet.getVersjon(), aktivitet.getVersjon())) {
            throw new VersjonsKonflikt();
        }
        if (skalIkkeKunneEndreAktivitet(orginalAktivitet)) {
            throw new IllegalArgumentException(
                    String.format("Kan ikke endre aktivitet aktivitet [%s]",
                            orginalAktivitet.getId())
            );
        }
    }

    private Boolean skalIkkeKunneEndreAktivitet(AktivitetData aktivitetData) {
        AktivitetStatus status = aktivitetData.getStatus();
        return AVBRUTT.equals(status) || FULLFORT.equals(status);
    }

    @Transactional
    public void settAktiviteterTilHistoriske(String aktoerId, Date sluttDato) {
        hentAktiviteterForAktorId(aktoerId)
                .stream()
                .filter(a -> skalBliHistorisk(a, sluttDato))
                .map(a -> a.withHistoriskDato(sluttDato))
                .forEach(aktivitetDAO::insertAktivitet);
    }

    private boolean skalBliHistorisk(AktivitetData aktivitetData, Date sluttdato) {
        return (aktivitetData.getHistoriskDato() == null || aktivitetData.getHistoriskDato().before(sluttdato))
                && aktivitetData.getOpprettetDato().before(sluttdato);
    }

}
