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

    public AktivitetData oppdaterStatus(long aktivitetId, AktivitetStatus status) {
        val gammelAktivitet = aktivitetDAO.hentAktivitet(aktivitetId);

        if (statusSkalIkkeKunneEndres(gammelAktivitet)) {
            throw new IllegalArgumentException(String.format("Kan ikke endre status til [%s] for aktivitet [%s]", status, aktivitetId));
        } else {
            aktivitetDAO.endreAktivitetStatus(aktivitetId, status);
            val endretBeskrivelse = String.format("livslopsendring, {\"fraStatus\": \"%s\", \"tilStatus\": \"%s\"}",
                    gammelAktivitet.getStatus().name(),
                    status.name());
            endringsLoggDAO.opprettEndringsLogg(aktivitetId, gammelAktivitet.getAktorId(), endretBeskrivelse);
        }

        return aktivitetDAO.hentAktivitet(aktivitetId);

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



