package no.nav.fo.veilarbaktivitet.ws.provider;

import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.*;
import no.nav.fo.veilarbaktivitet.ws.consumer.AktoerConsumer;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.*;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.OpprettNyEgenAktivitetRequest;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.OpprettNyEgenAktivitetResponse;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.OpprettNyStillingAktivitetRequest;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.OpprettNyStillingAktivitetResponse;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static java.util.Optional.of;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatusData.*;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.EGENAKTIVITET;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.JOBBSØKING;
import static no.nav.fo.veilarbaktivitet.domain.InnsenderData.BRUKER;
import static no.nav.fo.veilarbaktivitet.domain.InnsenderData.NAV;

@Component
class AktivitetsoversiktWebServiceTransformer {

    private static final BidiMap<InnsenderType, InnsenderData> innsenderMap = new DualHashBidiMap<>();

    static {
        innsenderMap.put(InnsenderType.BRUKER, BRUKER);
        innsenderMap.put(InnsenderType.NAV, NAV);
    }

    private static final BidiMap<Status, AktivitetStatusData> statusMap =
            new DualHashBidiMap<Status, AktivitetStatusData>() {{
                put(Status.AVBRUTT, AVBRUTT);
                put(Status.BRUKER_ER_INTERESSERT, BRUKER_ER_INTERESSERT);
                put(Status.FULLFOERT, FULLFØRT);
                put(Status.GJENNOMFOERT, GJENNOMFØRT);
                put(Status.PLANLAGT, PLANLAGT);
            }};

    private static final BidiMap<AktivitetType, AktivitetTypeData> typeMap =
            new DualHashBidiMap<AktivitetType, AktivitetTypeData>() {{
                put(AktivitetType.JOBBSOEKING, JOBBSØKING);
                put(AktivitetType.EGENAKTIVITET, EGENAKTIVITET);
            }};


    @Inject
    private AktoerConsumer aktoerConsumer;

    private String hentAktoerIdForIdent(String ident) {
        return aktoerConsumer.hentAktoerIdForIdent(ident)
                .orElseThrow(RuntimeException::new); // TODO Hvordan håndere dette?
    }

    private String hentIdentForAktorId(String aktorId) {
        return aktoerConsumer.hentIdentForAktørId(aktorId)
                .orElseThrow(RuntimeException::new); // TODO Hvordan håndere dette?
    }

    StillingsSoekAktivitet mapTilStillingAktivitetData(OpprettNyStillingAktivitetRequest request) {
        return new StillingsSoekAktivitet()
                .setStillingsoek(new StillingsoekData())
                .setAktivitet(mapTilAktivitetData(request.getStillingaktivitet().getAktivitet(), JOBBSØKING));
    }

    EgenAktivitetData mapTilEgenAktivitetData(OpprettNyEgenAktivitetRequest request) {
        return new EgenAktivitetData()
                .setAktivitet(mapTilAktivitetData(request.getEgenaktivitet().getAktivitet(), EGENAKTIVITET));
    }

    private AktivitetData mapTilAktivitetData(Aktivitet aktivitet, AktivitetTypeData type) {
        return new AktivitetData()
                .setAktorId(hentAktoerIdForIdent(aktivitet.getPersonIdent()))
                .setBeskrivelse(aktivitet.getBeskrivelse())
                .setAktivitetType(type)
                .setStatus(status(aktivitet))
                .setLagtInnAv(lagtInnAv(aktivitet));
    }

    Stillingaktivitet mapTilAktivitet(StillingsSoekAktivitet stillingsSoekAktivitet) {
        val stillingaktivitet = new Stillingaktivitet();
        stillingaktivitet.setAktivitet(mapTilAktivitet(stillingsSoekAktivitet.getAktivitet()));
        return stillingaktivitet;
    }

    Egenaktivitet mapTilAktivitet(EgenAktivitetData egenAktivitet) {
        val egenaktivitet = new Egenaktivitet();
        egenaktivitet.setAktivitet(mapTilAktivitet(egenAktivitet.getAktivitet()));


        //
        egenaktivitet.setType(EgenaktivitetType.values()[0]); // TODO dette må inn i datamodellen
        //

        return egenaktivitet;
    }

    private Aktivitet mapTilAktivitet(AktivitetData aktivitet) {
        val wsAktivitet = new Aktivitet();
        wsAktivitet.setAktivitetId(Long.toString(aktivitet.getId()));
        wsAktivitet.setPersonIdent(hentIdentForAktorId(aktivitet.getAktorId()));
        wsAktivitet.setStatus(statusMap.getKey(aktivitet.getStatus()));
        wsAktivitet.setType(typeMap.getKey(aktivitet.getAktivitetType()));
        wsAktivitet.setBeskrivelse(aktivitet.getBeskrivelse());
        wsAktivitet.setDelerMedNav(aktivitet.isDeleMedNav());
        return wsAktivitet;
    }

    private AktivitetStatusData status(Aktivitet aktivitet) {
        return statusMap.get(aktivitet.getStatus());
    }

    private InnsenderData lagtInnAv(Aktivitet aktivitet) {
        return of(aktivitet)
                .map(Aktivitet::getLagtInnAv)
                .map(Innsender::getType)
                .map(innsenderMap::get)
                .orElse(null); // TODO kreve lagt inn av?
    }

    OpprettNyEgenAktivitetResponse somOpprettNyEgenAktivitetResponse(Egenaktivitet egenaktivitet) {
        val opprettNyEgenAktivitetResponse = new OpprettNyEgenAktivitetResponse();

        opprettNyEgenAktivitetResponse.setEgenaktivitet(egenaktivitet);

        return opprettNyEgenAktivitetResponse;
    }

    OpprettNyStillingAktivitetResponse somOpprettNyStillingAktivitetResponse(Stillingaktivitet stillingaktivitet) {
        val opprettNyStillingAktivitetResponse = new OpprettNyStillingAktivitetResponse();

        opprettNyStillingAktivitetResponse.setStillingaktivitet(stillingaktivitet);

        return opprettNyStillingAktivitetResponse;
    }

    Endringslogg somEndringsLoggResponse(EndringsloggData endringsLogg) {
        val endringsLoggMelding = new Endringslogg();

        endringsLoggMelding.setEndringsBeskrivelse(endringsLogg.getEndringsBeskrivelse());
        endringsLoggMelding.setEndretAv(endringsLogg.getEndretAv());
        endringsLoggMelding.setEndretDato(endringsLogg.getEndretDato().toString());

        return endringsLoggMelding;
    }

}

