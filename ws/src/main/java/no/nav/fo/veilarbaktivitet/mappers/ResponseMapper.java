package no.nav.fo.veilarbaktivitet.mappers;

import lombok.val;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Aktivitet;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Aktivitetsplan;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.ArenaAktivitet;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.*;

import java.util.List;

public class ResponseMapper {
    public static EndreAktivitetStatusResponse mapTilEndreAktivitetStatusResponse(Aktivitet aktivitet) {
        val res = new EndreAktivitetStatusResponse();
        res.setAktivitet(aktivitet);
        return res;
    }

    public static EndreAktivitetEtikettResponse mapTilEndreAktivitetEtikettResponse(Aktivitet aktivitet) {
        val res = new EndreAktivitetEtikettResponse();
        res.setAktivitet(aktivitet);
        return res;
    }

    public static EndreAktivitetResponse mapTilEndreAktivitetResponse(Aktivitet aktivitet) {
        val res = new EndreAktivitetResponse();
        res.setAktivitet(aktivitet);
        return res;
    }

    public static OpprettNyAktivitetResponse mapTilOpprettNyAktivitetResponse(Aktivitet aktivitet) {
        val nyAktivitetResponse = new OpprettNyAktivitetResponse();
        nyAktivitetResponse.setAktivitet(aktivitet);
        return nyAktivitetResponse;
    }

    public static HentAktivitetsplanResponse mapTilHentAktivitetsplanResponse(List<Aktivitet> aktiviter) {
        val res = new HentAktivitetsplanResponse();
        val aktivitetsplan = new Aktivitetsplan();
        aktivitetsplan.getAktivitetListe().addAll(aktiviter);
        res.setAktivitetsplan(aktivitetsplan);
        return res;
    }

    public static HentAktivitetResponse maptTilHentAktivitetResponse(Aktivitet aktivitet) {
        val res = new HentAktivitetResponse();
        res.setAktivitet(aktivitet);
        return res;
    }

    public static HentArenaAktiviteterResponse mapTilHentArenaAktiviteterResponse(List<ArenaAktivitet> aktiviteter) {
        val res = new HentArenaAktiviteterResponse();
        res.getArenaaktiviteter().addAll(aktiviteter);
        return res;
    }

    public static HentAktivitetVersjonerResponse mapTilOpprettNyAktivitetResponse(List<Aktivitet> aktivitetVersjoner) {
        val res = new HentAktivitetVersjonerResponse();
        res.getAktivitetversjoner().addAll(aktivitetVersjoner);
        return res;
    }
}

