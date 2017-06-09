package no.nav.fo.veilarbaktivitet.mappers;

import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.EgenAktivitetData;
import no.nav.fo.veilarbaktivitet.domain.SokeAvtaleAktivitetData;
import no.nav.fo.veilarbaktivitet.domain.StillingsoekAktivitetData;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.*;

import java.util.Optional;

import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.wsStatus;
import static no.nav.fo.veilarbaktivitet.mappers.Helpers.*;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.xmlCalendar;

public class AktivitetWSMapper {
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
        wsAktivitet.setAvtalt(aktivitet.isAvtalt());
        wsAktivitet.setAvsluttetKommentar(aktivitet.getAvsluttetKommentar());


        Optional.ofNullable(aktivitet.getLagtInnAv()).ifPresent((lagtInnAv) -> {
            val innsender = new Innsender();
            innsender.setId(lagtInnAv.name());
            innsender.setType(innsenderMap.getKey(lagtInnAv));
            wsAktivitet.setLagtInnAv(innsender);
        });


        Optional.ofNullable(aktivitet.getStillingsSoekAktivitetData())
                .ifPresent(stillingsoekAktivitetData ->
                        wsAktivitet.setStillingAktivitet(mapTilStillingsAktivitet(stillingsoekAktivitetData)));
        Optional.ofNullable(aktivitet.getEgenAktivitetData())
                .ifPresent(egenAktivitetData ->
                        wsAktivitet.setEgenAktivitet(mapTilEgenAktivitet(egenAktivitetData)));
        Optional.ofNullable(aktivitet.getSokeAvtaleAktivitetData())
                .ifPresent(sokeAvtaleAktivitetData ->
                        wsAktivitet.setSokeavtale(mapTilSokeAvtaleAktivitet(sokeAvtaleAktivitetData)));

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
        egenaktivitet.setOppfolging(egenAktivitetData.getOppfolging());

        return egenaktivitet;
    }

    private static Sokeavtale mapTilSokeAvtaleAktivitet(SokeAvtaleAktivitetData sokeAvtaleAktivitetData) {
        val sokeAvtaleAtivitet = new Sokeavtale();

        sokeAvtaleAtivitet.setAvtaleOppfolging(sokeAvtaleAktivitetData.getAvtaleOppfolging());
        sokeAvtaleAtivitet.setAntall(sokeAvtaleAktivitetData.getAntall());

        return sokeAvtaleAtivitet;
    }

}
