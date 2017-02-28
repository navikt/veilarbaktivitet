package no.nav.fo.veilarbaktivitet.ws.provider;

import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.*;
import no.nav.fo.veilarbaktivitet.ws.consumer.AktoerConsumer;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.*;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.EndreAktivitetStatusResponse;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.OpprettNyAktivitetResponse;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Optional;

import static java.util.Optional.of;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatusData.*;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.*;
import static no.nav.fo.veilarbaktivitet.domain.InnsenderData.*;
import static no.nav.fo.veilarbaktivitet.domain.StillingsoekEtikettData.*;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.getDate;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.xmlCalendar;

@Component
class AktivitetsoversiktWebServiceTransformer {

    private static final BidiMap<InnsenderType, InnsenderData> innsenderMap =
            new DualHashBidiMap<InnsenderType, InnsenderData>() {{
                put(InnsenderType.BRUKER, BRUKER);
                put(InnsenderType.NAV, NAV);
            }};


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
                put(AktivitetType.JOBBSOEKING, JOBBSOEKING);
                put(AktivitetType.EGENAKTIVITET, EGENAKTIVITET);
            }};

    private static final BidiMap<Etikett, StillingsoekEtikettData> etikettMap =
            new DualHashBidiMap<Etikett, StillingsoekEtikettData>() {{
                put(Etikett.AVSLAG, AVSLAG);
                put(Etikett.INNKALDT_TIL_INTERVJU, INNKALT_TIL_INTERVJU);
                put(Etikett.JOBBTILBUD, JOBBTILBUD);
                put(Etikett.SOEKNAD_SENDT, SØKNAD_SENDT);
            }};


    @Inject
    private AktoerConsumer aktoerConsumer;

    private String hentAktoerIdForIdent(String ident) {
        return aktoerConsumer.hentAktoerIdForIdent(ident)
                .orElseThrow(RuntimeException::new); // TODO Hvordan håndere dette?
    }

    private String hentIdentForAktorId(String aktorId) {
        return aktoerConsumer.hentIdentForAktorId(aktorId)
                .orElseThrow(RuntimeException::new); // TODO Hvordan håndere dette?
    }

    public AktivitetData mapTilAktivitetData(Aktivitet aktivitet) {
        return new AktivitetData()
                .setAktorId(hentAktoerIdForIdent(aktivitet.getPersonIdent()))
                .setTittel(aktivitet.getTittel())
                .setFraDato(getDate(aktivitet.getFom()))
                .setTilDato(getDate(aktivitet.getTom()))
                .setBeskrivelse(aktivitet.getBeskrivelse())
                .setAktivitetType(AktivitetTypeData.valueOf(aktivitet.getType().name()))
                .setStatus(statusMap.get(aktivitet.getStatus()))
                .setLagtInnAv(lagtInnAv(aktivitet))
                .setLenke(aktivitet.getLenke())
                .setEgenAktivitetData(mapTilEgenAktivitetData(aktivitet.getEgenAktivitet()))
                .setStillingsSoekAktivitetData(mapTilStillingsoekAktivitetData(aktivitet.getStillingAktivitet()))
                ;
    }

    private StillingsoekAktivitetData mapTilStillingsoekAktivitetData(Stillingaktivitet stillingaktivitet) {
        return Optional.ofNullable(stillingaktivitet).map(stilling ->
                new StillingsoekAktivitetData()
                        .setArbeidsgiver(stilling.getArbeidsgiver())
                        .setKontaktPerson(stilling.getArbeidsgiver())
                        .setStillingsoekEtikett(etikettMap.get(stilling.getEtikett()))
                        .setStillingsTittel(stilling.getStillingstittel()))
                .orElse(null);
    }

    private EgenAktivitetData mapTilEgenAktivitetData(Egenaktivitet egenaktivitet) {
        return Optional.ofNullable(egenaktivitet)
                .map(egen -> new EgenAktivitetData())
                .orElse(null);
    }

    Aktivitet mapTilAktivitet(AktivitetData aktivitet) {
        val wsAktivitet = new Aktivitet();
        wsAktivitet.setAktivitetId(Long.toString(aktivitet.getId()));
        wsAktivitet.setTittel(aktivitet.getTittel());
        wsAktivitet.setTom(xmlCalendar(aktivitet.getTilDato()));
        wsAktivitet.setFom(xmlCalendar(aktivitet.getFraDato()));
        wsAktivitet.setPersonIdent(hentIdentForAktorId(aktivitet.getAktorId()));
        wsAktivitet.setStatus(statusMap.getKey(aktivitet.getStatus()));
        wsAktivitet.setType(typeMap.getKey(aktivitet.getAktivitetType()));
        wsAktivitet.setBeskrivelse(aktivitet.getBeskrivelse());
        wsAktivitet.setLenke(aktivitet.getLenke());
        wsAktivitet.setDelerMedNav(aktivitet.isDeleMedNav());
        wsAktivitet.setOpprettet(xmlCalendar(aktivitet.getOpprettetDato()));
        Optional.ofNullable(aktivitet.getStillingsSoekAktivitetData())
                .ifPresent(stillingsoekAktivitetData ->
                        wsAktivitet.setStillingAktivitet(mapTilStillingsAktivitet(stillingsoekAktivitetData)));
        Optional.ofNullable(aktivitet.getEgenAktivitetData())
                .ifPresent(egenAktivitetData ->
                        wsAktivitet.setEgenAktivitet(mapTilEgenAktivitet()));

        return wsAktivitet;
    }

    private Stillingaktivitet mapTilStillingsAktivitet(StillingsoekAktivitetData stillingsSoekAktivitet) {
        val stillingaktivitet = new Stillingaktivitet();

        stillingaktivitet.setArbeidsgiver(stillingsSoekAktivitet.getArbeidsgiver());
        stillingaktivitet.setEtikett(etikettMap.getKey(stillingsSoekAktivitet.getStillingsoekEtikett()));
        stillingaktivitet.setKontaktperson(stillingsSoekAktivitet.getKontaktPerson());
        stillingaktivitet.setStillingstittel(stillingaktivitet.getStillingstittel());

        return stillingaktivitet;
    }

    private Egenaktivitet mapTilEgenAktivitet() {
        val egenaktivitet = new Egenaktivitet();

        egenaktivitet.setType(EgenaktivitetType.values()[0]); // TODO dette må inn i datamodellen

        return egenaktivitet;
    }

    OpprettNyAktivitetResponse mapTilOpprettNyAktivitetResponse(Aktivitet aktivitet) {
        val nyAktivitetResponse = new OpprettNyAktivitetResponse();
        nyAktivitetResponse.setAktivitet(aktivitet);
        return nyAktivitetResponse;
    }

    private InnsenderData lagtInnAv(Aktivitet aktivitet) {
        return of(aktivitet)
                .map(Aktivitet::getLagtInnAv)
                .map(Innsender::getType)
                .map(innsenderMap::get)
                .orElse(null); // TODO kreve lagt inn av?
    }

    Endringslogg somEndringsLoggResponse(EndringsloggData endringsLogg) {
        val endringsLoggMelding = new Endringslogg();

        endringsLoggMelding.setEndringsBeskrivelse(endringsLogg.getEndringsBeskrivelse());
        endringsLoggMelding.setEndretAv(endringsLogg.getEndretAv());
        endringsLoggMelding.setEndretDato(endringsLogg.getEndretDato().toString());

        return endringsLoggMelding;
    }

    AktivitetStatusData mapTilAktivitetStatusData(Status status){
        return statusMap.get(status);
    }

    EndreAktivitetStatusResponse mapTilEndreAktivitetStatusResponse(Aktivitet aktivitet){
        val res = new EndreAktivitetStatusResponse();
        res.setAktivitet(aktivitet);
        return res;
    }

}

