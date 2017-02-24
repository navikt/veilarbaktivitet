package no.nav.fo.veilarbaktivitet.ws.provider;

import lombok.val;
import no.nav.fo.veilarbaktivitet.db.AktivitetDAO;
import no.nav.fo.veilarbaktivitet.db.EndringsLoggDAO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.ws.consumer.AktoerConsumer;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.binding.BehandleAktivitetsplanV1;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.binding.HentAktivitetsplanSikkerhetsbegrensing;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Aktivitetsplan;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Status;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.*;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.jws.WebService;

import static java.util.Optional.of;

@WebService
@Service
public class AktivitetsoversiktWebService implements BehandleAktivitetsplanV1 {

    @Inject
    AktoerConsumer aktoerConsumer;

    @Inject
    private AktivitetDAO aktivitetDAO;

    @Inject
    private EndringsLoggDAO endringsLoggDAO;

    @Inject
    private AktivitetsoversiktWebServiceTransformer aktivitetsoversiktWebServiceTransformer;

    @Override
    public OpprettNyAktivitetResponse opprettNyAktivitet(OpprettNyAktivitetRequest opprettNyAktivitetRequest) {

        return of(opprettNyAktivitetRequest)
                .map(OpprettNyAktivitetRequest::getAktivitet)
                .map(aktivitetsoversiktWebServiceTransformer::mapTilAktivitetData)
                .map(aktivitetDAO::opprettAktivitet)
                .map(aktivitetsoversiktWebServiceTransformer::mapTilAktivitet)
                .map(aktivitetsoversiktWebServiceTransformer::mapTilOpprettNyAktivitetResponse)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public HentAktivitetsplanResponse hentAktivitetsplan(HentAktivitetsplanRequest hentAktivitetsplanRequest) throws HentAktivitetsplanSikkerhetsbegrensing {
        String aktorId = hentAktoerIdForIdent(hentAktivitetsplanRequest.getPersonident());

        HentAktivitetsplanResponse wsHentAktiviteterResponse = new HentAktivitetsplanResponse();
        Aktivitetsplan aktivitetsplan = new Aktivitetsplan();
        wsHentAktiviteterResponse.setAktivitetsplan(aktivitetsplan);

        aktivitetDAO.hentAktiviteterForAktorId(aktorId)
                .stream()
                .map(aktivitetsoversiktWebServiceTransformer::mapTilAktivitet)
                .forEach(aktivitetsplan.getAktivitetListe()::add);

        return wsHentAktiviteterResponse;
    }

    @Override
    public HentEndringsLoggForAktivitetResponse hentEndringsLoggForAktivitet(HentEndringsLoggForAktivitetRequest hentEndringsLoggForAktivitetRequest) {
        val endringsloggResponse = new HentEndringsLoggForAktivitetResponse();
        val endringer = endringsloggResponse.getEndringslogg();

        of(hentEndringsLoggForAktivitetRequest)
                .map(HentEndringsLoggForAktivitetRequest::getAktivitetId)
                .map(Long::parseLong)
                .map(endringsLoggDAO::hentEndringdsloggForAktivitetId)
                .ifPresent((endringslist) -> endringslist.stream()
                        .map(aktivitetsoversiktWebServiceTransformer::somEndringsLoggResponse)
                        .forEach(endringer::add)
                );
        return endringsloggResponse;
    }

    @Override
    public EndreAktivitetStatusResponse endreAktivitetStatus(EndreAktivitetStatusRequest endreAktivitetStatusRequest) {
        return of(endreAktivitetStatusRequest)
                .map((req) -> endreAktivitetStatus(req.getAktivitetId(), req.getStatus()))
                .map(aktivitetsoversiktWebServiceTransformer::mapTilAktivitet)
                .map(aktivitetsoversiktWebServiceTransformer::mapTilEndreAktivitetStatusResponse)
                .orElseThrow(RuntimeException::new);
    }

    private AktivitetData endreAktivitetStatus(String aktivitetId, Status status){
        val id = Long.parseLong(aktivitetId);
        val statusData = aktivitetsoversiktWebServiceTransformer.mapTilAktivitetStatusData(status);

        val aktivtet = aktivitetDAO.endreAktivitetStatus(id, statusData);
        endringsLoggDAO.opprettEndringsLogg(id,
                "Test",
                String.format("Livsløpsstatus endret til %s ", status.name()));

        return aktivtet;
    }

    @Override
    public SlettAktivitetResponse slettAktivitet(SlettAktivitetRequest slettAktivitetRequest) {
        of(slettAktivitetRequest)
                .map(SlettAktivitetRequest::getAktivitetId)
                .map(Long::parseLong)
                .map(aktivitetDAO::slettAktivitet);
        return new SlettAktivitetResponse();
    }

    @Override
    public void ping() {
    }

    private String hentAktoerIdForIdent(String ident) {
        return aktoerConsumer.hentAktoerIdForIdent(ident)
                .orElseThrow(RuntimeException::new); // Hvordan håndere dette?
    }

}

