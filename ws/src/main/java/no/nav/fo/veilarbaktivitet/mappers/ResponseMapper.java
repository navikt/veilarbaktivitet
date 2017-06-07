package no.nav.fo.veilarbaktivitet.mappers;

import lombok.val;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Aktivitet;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.EndreAktivitetEtikettResponse;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.EndreAktivitetResponse;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.EndreAktivitetStatusResponse;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.OpprettNyAktivitetResponse;

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
}

