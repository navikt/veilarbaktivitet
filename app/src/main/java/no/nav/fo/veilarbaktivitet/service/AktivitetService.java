package no.nav.fo.veilarbaktivitet.service;

import lombok.val;
import no.nav.apiapp.feil.VersjonsKonflikt;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTransaksjonsType;
import no.nav.fo.veilarbsituasjon.rest.domain.AvsluttetOppfolgingFeedDTO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static java.util.Optional.ofNullable;

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
        kanEndreAktivitetGuard(orginalAktivitet);

        val nyAktivitet = orginalAktivitet
                .toBuilder()
                .status(aktivitet.getStatus())
                .lagtInnAv(aktivitet.getLagtInnAv())
                .avsluttetKommentar(aktivitet.getAvsluttetKommentar())
                .transaksjonsType(AktivitetTransaksjonsType.STATUS_ENDRET)
                .endretAv(endretAv)
                .build();

        aktivitetDAO.insertAktivitet(nyAktivitet);
    }

    public void oppdaterEtikett(AktivitetData aktivitet, String endretAv) {
        val orginalAktivitet = aktivitetDAO.hentAktivitet(aktivitet.getId());
        kanEndreAktivitetGuard(orginalAktivitet);

        val nyEtikett = aktivitet.getStillingsSoekAktivitetData().getStillingsoekEtikett();

        val orginalStillingsAktivitet = orginalAktivitet.getStillingsSoekAktivitetData();
        val nyStillingsAktivitet = orginalStillingsAktivitet.setStillingsoekEtikett(nyEtikett);

        val nyAktivitet = orginalAktivitet
                .toBuilder()
                .lagtInnAv(aktivitet.getLagtInnAv())
                .stillingsSoekAktivitetData(nyStillingsAktivitet)
                .transaksjonsType(AktivitetTransaksjonsType.ETIKETT_ENDRET)
                .endretAv(endretAv)
                .build();

        aktivitetDAO.insertAktivitet(nyAktivitet);
    }

    public void slettAktivitet(long aktivitetId) {
        aktivitetDAO.slettAktivitet(aktivitetId);
    }

    public void oppdaterAktivitetFrist(AktivitetData orginalAktivitet, AktivitetData aktivitetData, String endretAv) {
        kanEndreAktivitetGuard(orginalAktivitet);
        val oppdatertAktivitetMedNyFrist = orginalAktivitet
                .toBuilder()
                .lagtInnAv(aktivitetData.getLagtInnAv())
                .transaksjonsType(AktivitetTransaksjonsType.AVTALT_DATO_ENDRET)
                .tilDato(aktivitetData.getTilDato())
                .endretAv(endretAv)
                .build();
        aktivitetDAO.insertAktivitet(oppdatertAktivitetMedNyFrist);
    }

    public void oppdaterAktivitet(AktivitetData orginalAktivitet, AktivitetData aktivitet, String endretAv) {
        kanEndreAktivitetGuard(orginalAktivitet);

        val blittAvtalt = orginalAktivitet.isAvtalt() != aktivitet.isAvtalt();
        val transType = blittAvtalt ? AktivitetTransaksjonsType.AVTALT : AktivitetTransaksjonsType.DETALJER_ENDRET;

        val mergetAktivitetEndringer = orginalAktivitet
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
                .avtalt(aktivitet.isAvtalt());

        ofNullable(orginalAktivitet.getStillingsSoekAktivitetData()).ifPresent(
                stilling ->
                        stilling.setArbeidsgiver(aktivitet.getStillingsSoekAktivitetData().getArbeidsgiver())
                                .setArbeidssted(aktivitet.getStillingsSoekAktivitetData().getArbeidssted())
                                .setKontaktPerson(aktivitet.getStillingsSoekAktivitetData().getKontaktPerson())
                                .setStillingsTittel(aktivitet.getStillingsSoekAktivitetData().getStillingsTittel())
        );
        ofNullable(orginalAktivitet.getEgenAktivitetData()).ifPresent(
                egen -> egen
                        .setOppfolging(aktivitet.getEgenAktivitetData().getOppfolging())
                        .setHensikt(aktivitet.getEgenAktivitetData().getHensikt())
        );
        ofNullable(orginalAktivitet.getSokeAvtaleAktivitetData()).ifPresent(
                sokeAvtale -> sokeAvtale
                        .setAntallStillingerSokes(aktivitet.getSokeAvtaleAktivitetData().getAntallStillingerSokes())
                        .setAvtaleOppfolging(aktivitet.getSokeAvtaleAktivitetData().getAvtaleOppfolging())
        );
        ofNullable(orginalAktivitet.getIJobbAktivitetData()).ifPresent(
                iJobb -> iJobb
                        .setJobbStatusType(aktivitet.getIJobbAktivitetData().getJobbStatusType())
                        .setAnsettelsesforhold(aktivitet.getIJobbAktivitetData().getAnsettelsesforhold())
                        .setArbeidstid(aktivitet.getIJobbAktivitetData().getArbeidstid())
        );
        ofNullable(orginalAktivitet.getBehandlingAktivitetData()).ifPresent(
                behandling -> behandling
                        .setBehandlingType(aktivitet.getBehandlingAktivitetData().getBehandlingType())
                        .setBehandlingSted(aktivitet.getBehandlingAktivitetData().getBehandlingSted())
                        .setEffekt(aktivitet.getBehandlingAktivitetData().getEffekt())
                        .setBehandlingOppfolging(aktivitet.getBehandlingAktivitetData().getBehandlingOppfolging())
        );

        try {
            aktivitetDAO.insertAktivitet(mergetAktivitetEndringer.build());
        } catch (DuplicateKeyException e) {
            throw new VersjonsKonflikt();
        }
    }

    private void kanEndreAktivitetGuard(AktivitetData orginalAktivitet) {
        if (skalIkkeKunneEndreAktivitet(orginalAktivitet)) {
            throw new IllegalArgumentException(
                    String.format("Kan ikke endre aktivitet aktivitet [%s]",
                            orginalAktivitet.getId())
            );
        }
    }

    private Boolean skalIkkeKunneEndreAktivitet(AktivitetData aktivitetData) {
        return aktivitetData.getStatus() == AktivitetStatus.AVBRUTT ||
                aktivitetData.getStatus() == AktivitetStatus.FULLFORT;
    }

    @Transactional
    public void settAktiviteterTilHistoriske(AvsluttetOppfolgingFeedDTO element) {
        hentAktiviteterForAktorId(element.getAktoerid())
                .stream()
                .map(AktivitetData::toBuilder)
                .map(aktivitet -> aktivitet.historiskDato(element.getSluttdato()))
                .map(AktivitetData.AktivitetDataBuilder::build)
                .forEach(aktivitetDAO::insertAktivitet);
    }

}
