package no.nav.fo.veilarbaktivitet.service;

import lombok.val;
import no.nav.fo.veilarbaktivitet.db.AktivitetDAO;
import no.nav.fo.veilarbaktivitet.db.EndringsLoggDAO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatusData;
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

    public void slettAktivitet(String ident, long aktivitetId) {

//        Todo: use ident in delete
//        val aktorId = hentAktoerIdForIdent(ident);
        aktivitetDAO.slettAktivitet(aktivitetId);

    }

    public AktivitetData oppdaterStatus(String ident, long aktivitetId, AktivitetStatusData status) {
        //TODO use ident in change status
        val gammelAktivitet = aktivitetDAO.hentAktivitet(aktivitetId);

        if (!statusSkalIkkeKunneEndres(gammelAktivitet)) {
            aktivitetDAO.endreAktivitetStatus(aktivitetId, status);
            val endretBeskrivelse = String.format("livslopsendring, {\"fraStatus\": \"%s\", \"tilStatus\": \"%s\"}",
                    gammelAktivitet.getStatus().name(),
                    status.name());
            endringsLoggDAO.opprettEndringsLogg(aktivitetId, gammelAktivitet.getAktorId(), endretBeskrivelse);
        } //TODO return fault when updating an invalid aktivity or something

        return aktivitetDAO.hentAktivitet(aktivitetId);

    }

    private Boolean statusSkalIkkeKunneEndres(AktivitetData aktivitetData) {
        return aktivitetData.getStatus() == AktivitetStatusData.AVBRUTT ||
                aktivitetData.getStatus() == AktivitetStatusData.FULLFORT;
    }

    public List<EndringsloggData> hentEndringsloggForAktivitetId(String ident, long aktivitetId) {
        //TODO use ident in change status
        return endringsLoggDAO.hentEndringdsloggForAktivitetId(aktivitetId);
    }
}



