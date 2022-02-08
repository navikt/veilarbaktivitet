package no.nav.veilarbaktivitet.internapi;

import lombok.val;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.aktivitet.domain.MoteData;
import no.nav.veilarbaktivitet.internapi.model.Aktivitet;
import no.nav.veilarbaktivitet.internapi.model.Aktivitet.AktivitetTypeEnum;
import no.nav.veilarbaktivitet.internapi.model.Mote;

import java.time.ZoneOffset;

public class InternAktivitetMapper {

    public static Aktivitet mapTilAktivitet(AktivitetData aktivitetData) {
        AktivitetTypeData aktivitetType = aktivitetData.getAktivitetType();

        Aktivitet aktivitet = Aktivitet.builder()
                .kontorsperreEnhetId(aktivitetData.getKontorsperreEnhetId())
                .avtaltMedNav(aktivitetData.isAvtalt())
                .status(Aktivitet.StatusEnum.valueOf(aktivitetData.getStatus().name()))
                .beskrivelse(aktivitetData.getBeskrivelse())
                .tittel(aktivitetData.getTittel())
                .fraDato(aktivitetData.getFraDato().toInstant().atOffset(ZoneOffset.UTC))
                .tilDato(aktivitetData.getTilDato().toInstant().atOffset(ZoneOffset.UTC))
                .opprettetDato(aktivitetData.getOpprettetDato().toInstant().atOffset(ZoneOffset.UTC))
                .endretDato(aktivitetData.getOpprettetDato().toInstant().atOffset(ZoneOffset.UTC))
                .build();

        return switch (aktivitetType) {
            case EGENAKTIVITET -> mapTilMoteAktivitet(aktivitetData, aktivitet);
            case JOBBSOEKING -> mapTilMoteAktivitet(aktivitetData, aktivitet);
            case SOKEAVTALE -> mapTilMoteAktivitet(aktivitetData, aktivitet);
            case IJOBB -> mapTilMoteAktivitet(aktivitetData, aktivitet);
            case BEHANDLING -> mapTilMoteAktivitet(aktivitetData, aktivitet);
            case MOTE -> mapTilMoteAktivitet(aktivitetData, aktivitet);
            case SAMTALEREFERAT -> mapTilMoteAktivitet(aktivitetData, aktivitet);
            case STILLING_FRA_NAV -> mapTilMoteAktivitet(aktivitetData, aktivitet);
        };
    }

    public static Mote mapTilMoteAktivitet(AktivitetData aktivitetData, Aktivitet aktivitet) {
        MoteData moteData = aktivitetData.getMoteData();

        val mote = Mote.builder()
                .aktivitetType(AktivitetTypeEnum.MOTE)
                .adresse(moteData.getAdresse())
                .forberedelser(moteData.getForberedelser())
                .kanal(Mote.KanalEnum.valueOf(moteData.getKanal().name()))
                .referat(moteData.getReferat())
                .referatPublisert(moteData.isReferatPublisert())
                .build();

        return (Mote)merge(aktivitet, mote);
    }

    public static Aktivitet merge(Aktivitet base, Aktivitet aktivitet) {
        return aktivitet
                .kontorsperreEnhetId(base.getKontorsperreEnhetId())
                .avtaltMedNav(base.getAvtaltMedNav())
                .status(base.getStatus())
                .beskrivelse(base.getBeskrivelse())
                .tittel(base.getTittel())
                .fraDato(base.getFraDato())
                .tilDato(base.getTilDato())
                .opprettetDato(base.getOpprettetDato())
                .endretDato(base.getOpprettetDato());
    }
}
