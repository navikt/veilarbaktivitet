package no.nav.fo.veilarbaktivitet.mappers;

import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.*;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.*;

import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.wsStatus;
import static no.nav.fo.veilarbaktivitet.mappers.Helpers.*;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.xmlCalendar;

public class AktivitetWSMapper {

    public static Aktivitet mapTilAktivitet(AktivitetData aktivitet) {
        return mapTilAktivitet("", aktivitet);
    }

    public static Aktivitet mapTilAktivitet(String fnr, AktivitetData aktivitet) {
        val wsAktivitet = new Aktivitet();
        wsAktivitet.setPersonIdent(fnr);
        wsAktivitet.setAktivitetId(Long.toString(aktivitet.getId()));
        wsAktivitet.setVersjon(Long.toString(aktivitet.getVersjon()));
        wsAktivitet.setTittel(aktivitet.getTittel());
        wsAktivitet.setTom(xmlCalendar(aktivitet.getTilDato()));
        wsAktivitet.setFom(xmlCalendar(aktivitet.getFraDato()));
        wsAktivitet.setStatus(wsStatus(aktivitet.getStatus()));
        wsAktivitet.setType(typeMap.getKey(aktivitet.getAktivitetType()));
        wsAktivitet.setBeskrivelse(aktivitet.getBeskrivelse());
        wsAktivitet.setLenke(aktivitet.getLenke());
        wsAktivitet.setOpprettet(xmlCalendar(aktivitet.getOpprettetDato()));
        wsAktivitet.setEndret(xmlCalendar(aktivitet.getEndretDato()));
        wsAktivitet.setHistorisk(aktivitet.getHistoriskDato() != null);
        wsAktivitet.setAvtalt(aktivitet.isAvtalt());
        wsAktivitet.setAvsluttetKommentar(aktivitet.getAvsluttetKommentar());
        wsAktivitet.setTransaksjonsType(transaksjonsTypeMap.get(aktivitet.getTransaksjonsType()));


        ofNullable(aktivitet.getLagtInnAv()).ifPresent((lagtInnAv) -> {
            val innsender = new Innsender();
            innsender.setId(lagtInnAv.name());
            innsender.setType(innsenderMap.getKey(lagtInnAv));
            wsAktivitet.setLagtInnAv(innsender);
        });


        ofNullable(aktivitet.getStillingsSoekAktivitetData())
                .ifPresent(stillingsoekAktivitetData ->
                        wsAktivitet.setStillingAktivitet(mapTilStillingsAktivitet(stillingsoekAktivitetData)));
        ofNullable(aktivitet.getEgenAktivitetData())
                .ifPresent(egenAktivitetData ->
                        wsAktivitet.setEgenAktivitet(mapTilEgenAktivitet(egenAktivitetData)));
        ofNullable(aktivitet.getSokeAvtaleAktivitetData())
                .ifPresent(sokeAvtaleAktivitetData ->
                        wsAktivitet.setSokeavtale(mapTilSokeAvtaleAktivitet(sokeAvtaleAktivitetData)));
        ofNullable(aktivitet.getIJobbAktivitetData())
                .ifPresent(iJobbAktivitetData ->
                        wsAktivitet.setIjobb(mapTilIJobbAktivitet(iJobbAktivitetData)));
        ofNullable(aktivitet.getBehandlingAktivitetData())
                .ifPresent(behandlingAktivitetData ->
                        wsAktivitet.setBehandling(mapTilBehandlingAktivitet(behandlingAktivitetData)));
        ofNullable(aktivitet.getMoteData())
                .map(AktivitetWSMapper::mapTilMote)
                .ifPresent(wsAktivitet::setMote);

        return wsAktivitet;
    }

    private static Mote mapTilMote(MoteData moteData) {
        String referat = moteData.isReferatPublisert() ? moteData.getReferat() : null;

        Mote mote = new Mote();
        mote.setAdresse(moteData.getAdresse());
        mote.setForberedelser(moteData.getForberedelser());
        mote.setKanal(KanalDTO.getType(moteData.getKanal()));
        mote.setReferat(referat);
        mote.setErReferatPublisert(moteData.isReferatPublisert());
        return mote;
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
        egenaktivitet.setOppfolging(egenAktivitetData.getOppfolging());

        return egenaktivitet;
    }

    private static Sokeavtale mapTilSokeAvtaleAktivitet(SokeAvtaleAktivitetData sokeAvtaleAktivitetData) {
        val sokeAvtaleAtivitet = new Sokeavtale();

        sokeAvtaleAtivitet.setAvtaleOppfolging(sokeAvtaleAktivitetData.getAvtaleOppfolging());
        sokeAvtaleAtivitet.setAntall(sokeAvtaleAktivitetData.getAntallStillingerSokes());

        return sokeAvtaleAtivitet;
    }

    private static Ijobb mapTilIJobbAktivitet(IJobbAktivitetData iJobbAktivitetData) {
        val ijobb = new Ijobb();
        ijobb.setJobbStatus(jobbStatusTypeMap.getKey(iJobbAktivitetData.getJobbStatusType()));
        ijobb.setAnsettelsesforhold(iJobbAktivitetData.getAnsettelsesforhold());
        ijobb.setArbeidstid(iJobbAktivitetData.getArbeidstid());
        return ijobb;
    }

    private static Behandling mapTilBehandlingAktivitet(BehandlingAktivitetData behandlingAktivitetData) {
        val behandling = new Behandling();
        behandling.setBehandlingType(behandlingAktivitetData.getBehandlingType());
        behandling.setBehandlingSted(behandlingAktivitetData.getBehandlingSted());
        behandling.setEffekt(behandlingAktivitetData.getEffekt());
        behandling.setBehandlingOppfolging(behandlingAktivitetData.getBehandlingOppfolging());
        return behandling;
    }

}
