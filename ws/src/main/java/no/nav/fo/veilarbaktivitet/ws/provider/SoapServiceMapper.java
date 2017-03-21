package no.nav.fo.veilarbaktivitet.ws.provider;

import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.*;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.*;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.EndreAktivitetStatusResponse;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.OpprettNyAktivitetResponse;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static java.util.Optional.of;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.*;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.EGENAKTIVITET;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.JOBBSOEKING;
import static no.nav.fo.veilarbaktivitet.domain.EgenAktivitetTypeData.*;
import static no.nav.fo.veilarbaktivitet.domain.InnsenderData.BRUKER;
import static no.nav.fo.veilarbaktivitet.domain.InnsenderData.NAV;
import static no.nav.fo.veilarbaktivitet.domain.StillingsoekEtikettData.*;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.getDate;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.xmlCalendar;

@Component
class SoapServiceMapper {

    private static final BidiMap<InnsenderType, InnsenderData> innsenderMap =
            new DualHashBidiMap<InnsenderType, InnsenderData>() {{
                put(InnsenderType.BRUKER, BRUKER);
                put(InnsenderType.NAV, NAV);
            }};


    private static final BidiMap<Status, AktivitetStatus> statusMap =
            new DualHashBidiMap<Status, AktivitetStatus>() {{
                put(Status.AVBRUTT, AVBRUTT);
                put(Status.BRUKER_ER_INTERESSERT, BRUKER_ER_INTERESSERT);
                put(Status.FULLFOERT, FULLFORT);
                put(Status.GJENNOMFOERT, GJENNOMFORT);
                put(Status.PLANLAGT, PLANLAGT);
            }};

    private static final BidiMap<AktivitetType, AktivitetTypeData> typeMap =
            new DualHashBidiMap<AktivitetType, AktivitetTypeData>() {{
                put(AktivitetType.JOBBSOEKING, JOBBSOEKING);
                put(AktivitetType.EGENAKTIVITET, EGENAKTIVITET);
            }};

    private static final BidiMap<EgenaktivitetType, EgenAktivitetTypeData> egenTypeMap =
            new DualHashBidiMap<EgenaktivitetType, EgenAktivitetTypeData>() {{
                put(EgenaktivitetType.ANBEFALTE_AKTIVITETER, ANBEFALTE_AKTIVITETER);
                put(EgenaktivitetType.ANDRE_AKTIVITET, ANDRE_AKTIVITET);
                put(EgenaktivitetType.AVKLARE_AKTIVITETER, AVKLARE_AKTIVITETER);
                put(EgenaktivitetType.FINNE_ARBEIDSMULIGHETER, FINNE_ARBEIDSMULIGHETER);
            }};


    private static final BidiMap<Etikett, StillingsoekEtikettData> etikettMap =
            new DualHashBidiMap<Etikett, StillingsoekEtikettData>() {{
                put(Etikett.AVSLAG, AVSLAG);
                put(Etikett.INNKALDT_TIL_INTERVJU, INNKALT_TIL_INTERVJU);
                put(Etikett.JOBBTILBUD, JOBBTILBUD);
                put(Etikett.SOEKNAD_SENDT, SØKNAD_SENDT);
            }};



    static AktivitetData mapTilAktivitetData(Aktivitet aktivitet) {
        return new AktivitetData()
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

    private static StillingsoekAktivitetData mapTilStillingsoekAktivitetData(Stillingaktivitet stillingaktivitet) {
        return Optional.ofNullable(stillingaktivitet).map(stilling ->
                new StillingsoekAktivitetData()
                        .setArbeidsgiver(stilling.getArbeidsgiver())
                        .setKontaktPerson(stilling.getKontaktperson())
                        .setArbeidssted(stilling.getArbeidssted())
                        .setStillingsoekEtikett(etikettMap.get(stilling.getEtikett()))
                        .setStillingsTittel(stilling.getStillingstittel()))
                .orElse(null);
    }

    private static EgenAktivitetData mapTilEgenAktivitetData(Egenaktivitet egenaktivitet) {
        return Optional.ofNullable(egenaktivitet)
                .map(egen ->
                        new EgenAktivitetData()
                                .setHensikt(egen.getHensikt())
                                .setType(egenTypeMap.get(egen.getType())))
                .orElse(null);
    }

    static Aktivitet mapTilAktivitet(String fnr, AktivitetData aktivitet) {
        val wsAktivitet = new Aktivitet();
        wsAktivitet.setPersonIdent(fnr);
        wsAktivitet.setAktivitetId(Long.toString(aktivitet.getId()));
        wsAktivitet.setTittel(aktivitet.getTittel());
        wsAktivitet.setTom(xmlCalendar(aktivitet.getTilDato()));
        wsAktivitet.setFom(xmlCalendar(aktivitet.getFraDato()));
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
                        wsAktivitet.setEgenAktivitet(mapTilEgenAktivitet(egenAktivitetData)));

        return wsAktivitet;
    }

    private static Stillingaktivitet mapTilStillingsAktivitet(StillingsoekAktivitetData stillingsSoekAktivitet) {
        val stillingaktivitet = new Stillingaktivitet();

        stillingaktivitet.setArbeidsgiver(stillingsSoekAktivitet.getArbeidsgiver());
        stillingaktivitet.setEtikett(etikettMap.getKey(stillingsSoekAktivitet.getStillingsoekEtikett()));
        stillingaktivitet.setKontaktperson(stillingsSoekAktivitet.getKontaktPerson());
        stillingaktivitet.setStillingstittel(stillingsSoekAktivitet.getStillingsTittel());
        stillingaktivitet.setArbeidssted(stillingsSoekAktivitet.getArbeidssted());

        return stillingaktivitet;
    }

    private static Egenaktivitet mapTilEgenAktivitet(EgenAktivitetData egenAktivitetData) {
        val egenaktivitet = new Egenaktivitet();

        egenaktivitet.setHensikt(egenAktivitetData.getHensikt());
        egenaktivitet.setType(egenTypeMap.getKey(egenAktivitetData.getType()));

        return egenaktivitet;
    }

    static OpprettNyAktivitetResponse mapTilOpprettNyAktivitetResponse(Aktivitet aktivitet) {
        val nyAktivitetResponse = new OpprettNyAktivitetResponse();
        nyAktivitetResponse.setAktivitet(aktivitet);
        return nyAktivitetResponse;
    }

    private static InnsenderData lagtInnAv(Aktivitet aktivitet) {
        return of(aktivitet)
                .map(Aktivitet::getLagtInnAv)
                .map(Innsender::getType)
                .map(innsenderMap::get)
                .orElse(null); // TODO kreve lagt inn av?
    }

    static Endringslogg somEndringsLoggResponse(EndringsloggData endringsLogg) {
        val endringsLoggMelding = new Endringslogg();

        endringsLoggMelding.setEndringsBeskrivelse(endringsLogg.getEndringsBeskrivelse());
        endringsLoggMelding.setEndretAv(endringsLogg.getEndretAv());
        endringsLoggMelding.setEndretDato(xmlCalendar(endringsLogg.getEndretDato()));

        return endringsLoggMelding;
    }

    static AktivitetStatus mapTilAktivitetStatusData(Status status){
        return statusMap.get(status);
    }

    static EndreAktivitetStatusResponse mapTilEndreAktivitetStatusResponse(Aktivitet aktivitet){
        val res = new EndreAktivitetStatusResponse();
        res.setAktivitet(aktivitet);
        return res;
    }

}

