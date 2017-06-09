package no.nav.fo.veilarbaktivitet.service;

import lombok.val;
import no.nav.apiapp.feil.VersjonsKonflikt;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.fo.veilarbaktivitet.domain.TransaksjonsTypeData;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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

    public long opprettAktivitet(String aktorId, AktivitetData aktivitet) {
        val aktivitetId = aktivitetDAO.getNextUniqueAktivitetId();
        val nyAktivivitet = aktivitet
                .toBuilder()
                .id(aktivitetId)
                .aktorId(aktorId)
                .transaksjonsTypeData(TransaksjonsTypeData.OPPRETTET)
                .opprettetDato(new Date())
                .build();

        aktivitetDAO.insertAktivitet(nyAktivivitet);
        return aktivitetId;
    }


    public void oppdaterStatus(AktivitetData aktivitet) {
        val orginalAktivitet = aktivitetDAO.hentAktivitet(aktivitet.getId());
        kanEndreAktivitetGuard(orginalAktivitet);

        val nyAktivitet = orginalAktivitet
                .toBuilder()
                .status(aktivitet.getStatus())
                .avsluttetKommentar(aktivitet.getAvsluttetKommentar())
                .transaksjonsTypeData(TransaksjonsTypeData.STATUS_ENDRET)
                .build();

        aktivitetDAO.insertAktivitet(nyAktivitet);
    }

    public void oppdaterEtikett(AktivitetData aktivitet) {
        val orginalAktivitet = aktivitetDAO.hentAktivitet(aktivitet.getId());
        kanEndreAktivitetGuard(orginalAktivitet);

        val nyEtikett = aktivitet.getStillingsSoekAktivitetData().getStillingsoekEtikett();

        val orginalStillingsAktivitet = orginalAktivitet.getStillingsSoekAktivitetData();
        val nyStillingsAktivitet = orginalStillingsAktivitet.setStillingsoekEtikett(nyEtikett);

        val nyAktivitet = orginalAktivitet
                .toBuilder()
                .stillingsSoekAktivitetData(nyStillingsAktivitet)
                .transaksjonsTypeData(TransaksjonsTypeData.STATUS_ENDRET)
                .build();

        aktivitetDAO.insertAktivitet(nyAktivitet);
    }

    public void slettAktivitet(long aktivitetId) {
        aktivitetDAO.slettAktivitet(aktivitetId);
    }

    public void oppdaterAktivitetFrist(AktivitetData orginalAktivitet, Date frist) {
        kanEndreAktivitetGuard(orginalAktivitet);
        val oppdatertAktivitetMedNyFrist = orginalAktivitet
                .toBuilder()
                .transaksjonsTypeData(TransaksjonsTypeData.AVTALT_DATO_ENDRET)
                .tilDato(frist)
                .build();
        aktivitetDAO.insertAktivitet(oppdatertAktivitetMedNyFrist);
    }

    public void oppdaterAktivitet(AktivitetData orginalAktivitet, AktivitetData aktivitet) {
        kanEndreAktivitetGuard(orginalAktivitet);

        val blittAvtalt = orginalAktivitet.isAvtalt() != aktivitet.isAvtalt();
        val transType = blittAvtalt ? TransaksjonsTypeData.AVTALT : TransaksjonsTypeData.DETALJER_ENDRET;

        val mergetAktivitetEndringer = orginalAktivitet
                .toBuilder()
                .fraDato(aktivitet.getFraDato())
                .tilDato(aktivitet.getTilDato())
                .tittel(aktivitet.getTittel())
                .beskrivelse(aktivitet.getBeskrivelse())
                .avsluttetKommentar(aktivitet.getAvsluttetKommentar())
                .lenke(aktivitet.getLenke())
                .transaksjonsTypeData(transType)
                .versjon(aktivitet.getVersjon())
                .avtalt(aktivitet.isAvtalt());

        Optional.ofNullable(orginalAktivitet.getStillingsSoekAktivitetData()).ifPresent(
                stilling ->
                        stilling.setArbeidsgiver(aktivitet.getStillingsSoekAktivitetData().getArbeidsgiver())
                                .setArbeidssted(aktivitet.getStillingsSoekAktivitetData().getArbeidssted())
                                .setKontaktPerson(aktivitet.getStillingsSoekAktivitetData().getKontaktPerson())
                                .setStillingsTittel(aktivitet.getStillingsSoekAktivitetData().getStillingsTittel())
        );
        Optional.ofNullable(orginalAktivitet.getEgenAktivitetData()).ifPresent(
                egen -> egen
                        .setOppfolging(aktivitet.getEgenAktivitetData().getOppfolging())
                        .setHensikt(aktivitet.getEgenAktivitetData().getHensikt())
        );
        Optional.ofNullable(orginalAktivitet.getSokeAvtaleAktivitetData()).ifPresent(
                sokeAvtale -> sokeAvtale
                        .setAntall(aktivitet.getSokeAvtaleAktivitetData().getAntall())
                        .setAvtaleOppfolging(aktivitet.getSokeAvtaleAktivitetData().getAvtaleOppfolging())
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
}
