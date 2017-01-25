package no.nav.fo.veilarbaktivitet.ws.provider;

import no.nav.fo.veilarbaktivitet.domain.Aktivitet;
import no.nav.fo.veilarbaktivitet.domain.*;
import no.nav.fo.veilarbaktivitet.domain.AktivitetType;
import no.nav.fo.veilarbaktivitet.domain.Innsender;
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
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.*;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetType.EGENAKTIVITET;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetType.JOBBSØKING;
import static no.nav.fo.veilarbaktivitet.domain.Innsender.BRUKER;
import static no.nav.fo.veilarbaktivitet.domain.Innsender.NAV;

@Component
public class AktivitetsoversiktWebServiceTransformer {

    private static final BidiMap<InnsenderType, Innsender> innsenderMap = new DualHashBidiMap<>();

    static {
        innsenderMap.put(InnsenderType.BRUKER, BRUKER);
        innsenderMap.put(InnsenderType.NAV, NAV);
    }

    private static final BidiMap<Status, AktivitetStatus> statusMap = new DualHashBidiMap<Status, AktivitetStatus>() {{
        put(Status.AVBRUTT, AVBRUTT);
        put(Status.BRUKER_ER_INTERESSERT, BRUKER_ER_INTERESSERT);
        put(Status.FULLFOERT, FULLFØRT);
        put(Status.GJENNOMFOERT, GJENNOMFØRT);
        put(Status.PLANLAGT, PLANLAGT);
    }};

    private static final BidiMap<no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.AktivitetType, AktivitetType> typeMap = new DualHashBidiMap<no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.AktivitetType, AktivitetType>() {{
        put(no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.AktivitetType.JOBBSØKING, JOBBSØKING);
        put(no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.AktivitetType.EGENAKTIVITET, EGENAKTIVITET);
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

    public StillingsSoekAktivitet somStillingAktivitet(OpprettNyStillingAktivitetRequest request) {
        return new StillingsSoekAktivitet()
                .setStillingsoek(new Stillingsoek())
                .setAktivitet(somAktivitet(request.getStillingaktivitet().getAktivitet(), JOBBSØKING));
    }

    public EgenAktivitet somEgenAktivitet(OpprettNyEgenAktivitetRequest request) {

        return new EgenAktivitet()
                .setAktivitet(somAktivitet(request.getEgenaktivitet().getAktivitet(), EGENAKTIVITET));
    }

    private Aktivitet somAktivitet(no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Aktivitet aktivitet, AktivitetType type) {
        return new Aktivitet()
                .setAktorId(hentAktoerIdForIdent(aktivitet.getPersonIdent()))
                .setBeskrivelse(aktivitet.getBeskrivelse())
                .setAktivitetType(type)
                .setStatus(status(aktivitet))
                .setLagtInnAv(lagtInnAv(aktivitet));
    }

    public Stillingaktivitet somWSAktivitet(StillingsSoekAktivitet stillingsSoekAktivitet) {
        Stillingaktivitet stillingaktivitet = new Stillingaktivitet();
        stillingaktivitet.setAktivitet(somWSAktivitet(stillingsSoekAktivitet.getAktivitet()));
        return stillingaktivitet;
    }

    public Egenaktivitet somWSAktivitet(EgenAktivitet egenAktivitet) {
        Egenaktivitet egenaktivitet = new Egenaktivitet();
        egenaktivitet.setAktivitet(somWSAktivitet(egenAktivitet.getAktivitet()));

        //
        egenaktivitet.setTag(EgenaktivitetTag.values()[0]); // TODO dette må inn i datamodellen
        //

        return egenaktivitet;
    }

    private no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Aktivitet somWSAktivitet(Aktivitet aktivitet) {
        no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Aktivitet wsAktivitet = new no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Aktivitet();
        wsAktivitet.setAktivitetId(Long.toString(aktivitet.getId()));
        wsAktivitet.setPersonIdent(hentIdentForAktorId(aktivitet.getAktorId()));
        wsAktivitet.setStatus(statusMap.getKey(aktivitet.getStatus()));
        wsAktivitet.setType(typeMap.getKey(aktivitet.getAktivitetType()));
        wsAktivitet.setBeskrivelse(aktivitet.getBeskrivelse());
        wsAktivitet.setDelerMedNav(aktivitet.isDeleMedNav());
        return wsAktivitet;
    }

    private AktivitetStatus status(no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Aktivitet aktivitet) {
        return statusMap.get(aktivitet.getStatus());
    }

    private Innsender lagtInnAv(no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Aktivitet aktivitet) {
        return of(aktivitet)
                .map(no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Aktivitet::getLagtInnAv)
                .map(no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Innsender::getType)
                .map(innsenderMap::get)
                .orElse(null); // TODO kreve lagt inn av?
    }

    public OpprettNyEgenAktivitetResponse somOpprettNyEgenAktivitetResponse(Egenaktivitet egenaktivitet) {
        OpprettNyEgenAktivitetResponse opprettNyEgenAktivitetResponse = new OpprettNyEgenAktivitetResponse();
        opprettNyEgenAktivitetResponse.setEgenaktivitet(egenaktivitet);
        return opprettNyEgenAktivitetResponse;
    }

    public OpprettNyStillingAktivitetResponse somOpprettNyStillingAktivitetResponse(Stillingaktivitet stillingaktivitet) {
        OpprettNyStillingAktivitetResponse opprettNyStillingAktivitetResponse = new OpprettNyStillingAktivitetResponse();
        opprettNyStillingAktivitetResponse.setStillingaktivitet(stillingaktivitet);
        return opprettNyStillingAktivitetResponse;
    }

}

