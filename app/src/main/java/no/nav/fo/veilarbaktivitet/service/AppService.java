package no.nav.fo.veilarbaktivitet.service;

import lombok.val;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.fo.veilarbaktivitet.db.dao.EndringsLoggDAO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.fo.veilarbaktivitet.domain.EndringsloggData;
import no.nav.fo.veilarbaktivitet.ws.consumer.AktoerConsumer;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
public class AppService {

    @Inject
    private AktoerConsumer aktoerConsumer;

    @Inject
    private AktivitetDAO aktivitetDAO;

    @Inject
    private EndringsLoggDAO endringsLoggDAO;

    private String hentAktoerIdForIdent(String ident) {
        return aktoerConsumer.hentAktoerIdForIdent(ident)
                .orElseThrow(RuntimeException::new); // Hvordan håndere dette?
    }

    public List<AktivitetData> hentAktiviteterForIdent(String ident) {
        val aktorId = hentAktoerIdForIdent(ident);
        return aktivitetDAO.hentAktiviteterForAktorId(aktorId);
    }

    public AktivitetData opprettNyAktivtet(String ident, AktivitetData aktivitetData) {
        val aktorId = hentAktoerIdForIdent(ident);
        return aktivitetDAO.opprettAktivitet(aktivitetData.setAktorId(aktorId));
    }

    public void slettAktivitet(long aktivitetId) {
        aktivitetDAO.slettAktivitet(aktivitetId);
    }

    public AktivitetData oppdaterAktivitet(AktivitetData aktivitetData){
        return aktivitetDAO.oppdaterAktivitet(aktivitetData);
    }

    public AktivitetData hentAktivitet(long id){
        return aktivitetDAO.hentAktivitet(id);
    }

    public AktivitetData oppdaterStatus(AktivitetData aktivitet) {
        val gammelAktivitet = hentAktivitet(aktivitet.getId());

        if (statusSkalIkkeKunneEndres(gammelAktivitet)) {
            throw new IllegalArgumentException(
                    String.format("Kan ikke endre status til [%s] for aktivitet [%s]",
                            aktivitet.getStatus(), aktivitet.getId())
            );
        } else {
            aktivitetDAO.endreAktivitetStatus(
                    aktivitet.getId(),
                    aktivitet.getStatus(),
                    aktivitet.getAvsluttetKommentar()
            );
            val endretBeskrivelse = String.format("livslopsendring, {\"fraStatus\": \"%s\", \"tilStatus\": \"%s\"}",
                    gammelAktivitet.getStatus().name(),
                    aktivitet.getStatus());
            endringsLoggDAO.opprettEndringsLogg(aktivitet.getId(), gammelAktivitet.getAktorId(), endretBeskrivelse);
        }

        return aktivitetDAO.hentAktivitet(aktivitet.getId());

    }

    private Boolean statusSkalIkkeKunneEndres(AktivitetData aktivitetData) {
        return aktivitetData.getStatus() == AktivitetStatus.AVBRUTT ||
                aktivitetData.getStatus() == AktivitetStatus.FULLFORT;
    }

    public List<EndringsloggData> hentEndringsloggForAktivitetId(long aktivitetId) {
        //TODO use ident in change status
        return endringsLoggDAO.hentEndringdsloggForAktivitetId(aktivitetId);
    }
}



