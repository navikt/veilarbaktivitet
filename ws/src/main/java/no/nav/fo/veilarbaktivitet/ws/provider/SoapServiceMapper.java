package no.nav.fo.veilarbaktivitet.ws.provider;

import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.*;
import no.nav.fo.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.fo.veilarbaktivitet.domain.arena.ArenaAktivitetTypeDTO;
import no.nav.fo.veilarbaktivitet.domain.arena.ArenaStatusDTO;
import no.nav.fo.veilarbaktivitet.domain.arena.MoteplanDTO;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.*;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.EndreAktivitetEtikettResponse;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.EndreAktivitetResponse;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.EndreAktivitetStatusResponse;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.OpprettNyAktivitetResponse;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.aktivitetStatus;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.wsStatus;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.*;
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


    private static final BidiMap<AktivitetType, AktivitetTypeData> typeMap =
            new DualHashBidiMap<AktivitetType, AktivitetTypeData>() {{
                put(AktivitetType.JOBBSOEKING, JOBBSOEKING);
                put(AktivitetType.EGENAKTIVITET, EGENAKTIVITET);
                put(AktivitetType.SOKEAVTALE, SOKEAVTALE);
            }};


    private static final BidiMap<Etikett, StillingsoekEtikettData> etikettMap =
            new DualHashBidiMap<Etikett, StillingsoekEtikettData>() {{
                put(Etikett.AVSLAG, AVSLAG);
                put(Etikett.INNKALDT_TIL_INTERVJU, INNKALT_TIL_INTERVJU);
                put(Etikett.JOBBTILBUD, JOBBTILBUD);
                put(Etikett.SOEKNAD_SENDT, SOKNAD_SENDT);
            }};


    static AktivitetData mapTilAktivitetData(Aktivitet aktivitet) {

        return new AktivitetData()
                .setId(Optional.ofNullable(aktivitet.getAktivitetId())
                        .filter((id) -> !id.isEmpty())
                        .map(Long::parseLong)
                        .orElse(null)
                )
                .setVersjon(ofNullable(aktivitet.getVersjon())
                        .filter((versjon) -> !versjon.isEmpty())
                        .map(Long::parseLong)
                        .orElse(0L)
                )
                .setTittel(aktivitet.getTittel())
                .setFraDato(getDate(aktivitet.getFom()))
                .setTilDato(getDate(aktivitet.getTom()))
                .setBeskrivelse(aktivitet.getBeskrivelse())
                .setAktivitetType(AktivitetTypeData.valueOf(aktivitet.getType().name()))
                .setStatus(aktivitetStatus(aktivitet.getStatus()))
                .setLagtInnAv(lagtInnAv(aktivitet))
                .setLenke(aktivitet.getLenke())
                .setOpprettetDato(getDate(aktivitet.getOpprettet()))
                .setAvtalt(Optional.ofNullable(aktivitet.getAvtalt()).orElse(false))
                .setAvsluttetKommentar(aktivitet.getAvsluttetKommentar())
                .setEgenAktivitetData(mapTilEgenAktivitetData(aktivitet.getEgenAktivitet()))
                .setStillingsSoekAktivitetData(mapTilStillingsoekAktivitetData(aktivitet.getStillingAktivitet()))
                .setSokeAvtaleAktivitetData(mapTilSokeavtaleAktivitetData(aktivitet.getSokeavtale()))
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
                                .setOppfolging(egen.getOppfolging()))
                .orElse(null);
    }

    private static SokeAvtaleAktivitetData mapTilSokeavtaleAktivitetData(Sokeavtale sokeavtaleAktivitet) {
        return Optional.ofNullable(sokeavtaleAktivitet)
                .map(sokeavtale ->
                        new SokeAvtaleAktivitetData()
                                .setAntall(sokeavtaleAktivitet.getAntall())
                                .setAvtaleOppfolging(sokeavtaleAktivitet.getAvtaleOppfolging())
                ).orElse(null);
    }

    static Aktivitet mapTilAktivitet(String fnr, AktivitetData aktivitet) {
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

    static EndreAktivitetStatusResponse mapTilEndreAktivitetStatusResponse(Aktivitet aktivitet) {
        val res = new EndreAktivitetStatusResponse();
        res.setAktivitet(aktivitet);
        return res;
    }

    static EndreAktivitetEtikettResponse mapTilEndreAktivitetEtikettResponse(Aktivitet aktivitet) {
        val res = new EndreAktivitetEtikettResponse();
        res.setAktivitet(aktivitet);
        return res;
    }


    static EndreAktivitetResponse mapTilEndreAktivitetResponse(Aktivitet aktivitet) {
        val res = new EndreAktivitetResponse();
        res.setAktivitet(aktivitet);
        return res;
    }

    private static final BidiMap<ArenaAktivitetType, ArenaAktivitetTypeDTO> arenaTypeMap =
            new DualHashBidiMap<ArenaAktivitetType, ArenaAktivitetTypeDTO>() {{
                put(ArenaAktivitetType.GRUPPEAKTIVITET, ArenaAktivitetTypeDTO.GRUPPEAKTIVITET);
                put(ArenaAktivitetType.TILTAKSAKTIVITET, ArenaAktivitetTypeDTO.TILTAKSAKTIVITET);
                put(ArenaAktivitetType.UTDANNINGSAKTIVITET, ArenaAktivitetTypeDTO.UTDANNINGSAKTIVITET);
            }};

    private static final BidiMap<ArenaEtikett, ArenaStatusDTO> arenaEtikettMap =
            new DualHashBidiMap<ArenaEtikett, ArenaStatusDTO>() {{
                put(ArenaEtikett.AKTUELL, ArenaStatusDTO.AKTUELL);
                put(ArenaEtikett.AVSLAG, ArenaStatusDTO.AVSLAG);
                put(ArenaEtikett.IKKAKTUELL, ArenaStatusDTO.IKKAKTUELL);
                put(ArenaEtikett.IKKEM, ArenaStatusDTO.IKKEM);
                put(ArenaEtikett.INFOMOETE, ArenaStatusDTO.INFOMOETE);
                put(ArenaEtikett.JATAKK, ArenaStatusDTO.JATAKK);
                put(ArenaEtikett.NEITAKK, ArenaStatusDTO.NEITAKK);
                put(ArenaEtikett.TILBUD, ArenaStatusDTO.TILBUD);
                put(ArenaEtikett.VENTELISTE, ArenaStatusDTO.VENTELISTE);
            }};

    static ArenaAktivitet mapTilArenaAktivitet(ArenaAktivitetDTO dto) {
        val arena = new ArenaAktivitet();
        arena.setAktivitetId(dto.getId());
        arena.setTittel(dto.getTittel());
        arena.setStatus(wsStatus(dto.getStatus()));
        arena.setType(arenaTypeMap.getKey(dto.getType()));
        arena.setBeskrivelse(dto.getBeskrivelse());
        arena.setFom(xmlCalendar(dto.getFraDato()));
        arena.setTom(xmlCalendar(dto.getTilDato()));
        arena.setOpprettetDato(xmlCalendar(dto.getOpprettetDato()));
        arena.setAvtalt(dto.isAvtalt());
        arena.setEtikett(arenaEtikettMap.getKey(dto.getEtikett()));
        arena.setDeltakelseProsent(dto.getDeltakelseProsent());
        arena.setTiltaksnavn(dto.getTiltaksnavn());
        arena.setTiltakLokaltNavn(dto.getTiltakLokaltNavn());
        arena.setArrangoer(dto.getArrangoer());
        arena.setBedriftsnummer(dto.getBedriftsnummer());
        arena.setAntallDagerPerUke(dto.getAntallDagerPerUke());
        arena.setStatusSistEndret(xmlCalendar(dto.getStatusSistEndret()));

        Optional.ofNullable(dto.getMoeteplanListe())
                .ifPresent(moteListe ->
                        arena.getMoeteplanListe().addAll(moteListe.stream()
                                .map(SoapServiceMapper::mapTilMotePlan)
                                .collect(Collectors.toList()))
                );

        return arena;
    }

    private static Moeteplan mapTilMotePlan(MoteplanDTO dto) {
        val mote = new Moeteplan();
        mote.setSluttDato(xmlCalendar(dto.getSluttDato()));
        mote.setStartDato(xmlCalendar(dto.getStartDato()));
        mote.setSted(dto.getSted());

        return mote;

    }
}

