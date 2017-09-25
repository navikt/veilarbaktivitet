package no.nav.fo.veilarbaktivitet.provider;

import lombok.val;
import no.nav.apiapp.soap.SoapTjeneste;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTransaksjonsType;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData;
import no.nav.fo.veilarbaktivitet.mappers.AktivitetDataMapper;
import no.nav.fo.veilarbaktivitet.mappers.AktivitetWSMapper;
import no.nav.fo.veilarbaktivitet.mappers.ArenaAktivitetWSMapper;
import no.nav.fo.veilarbaktivitet.mappers.ResponseMapper;
import no.nav.fo.veilarbaktivitet.service.AktivitetWSAppService;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.binding.BehandleAktivitetsplanV1;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.binding.HentAktivitetsplanSikkerhetsbegrensing;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Aktivitet;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.*;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.fo.veilarbaktivitet.mappers.AktivitetWSMapper.mapTilAktivitet;

@Service
@SoapTjeneste("/Aktivitet")
public class AktivitetsplanWS implements BehandleAktivitetsplanV1 {

    private final AktivitetWSAppService appService;

    @Inject
    public AktivitetsplanWS(AktivitetWSAppService appService) {
        this.appService = appService;
    }

    @Override
    public HentAktivitetsplanResponse hentAktivitetsplan(HentAktivitetsplanRequest hentAktivitetsplanRequest) throws HentAktivitetsplanSikkerhetsbegrensing {
        return Optional.of(hentAktivitetsplanRequest)
                .map(HentAktivitetsplanRequest::getPersonident)
                .map(fnr -> appService
                        .hentAktiviteterForIdent(fnr)
                        .stream()
                        .map(aktivitet -> mapTilAktivitet(fnr, aktivitet))
                        .collect(Collectors.toList()))
                .map(ResponseMapper::mapTilHentAktivitetsplanResponse)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public HentArenaAktiviteterResponse hentArenaAktiviteter(HentArenaAktiviteterRequest hentArenaAktiviteterRequest) {
        return Optional.of(hentArenaAktiviteterRequest)
                .map(HentArenaAktiviteterRequest::getPersonident)
                .map(appService::hentArenaAktiviteter)
                .map(aktivitetList -> aktivitetList
                        .stream()
                        .map(ArenaAktivitetWSMapper::mapTilArenaAktivitet)
                        .collect(Collectors.toList()))
                .map(ResponseMapper::mapTilHentArenaAktiviteterResponse)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public HentAktivitetResponse hentAktivitet(HentAktivitetRequest hentAktivitetRequest) {
        return Optional.of(hentAktivitetRequest)
                .map(HentAktivitetRequest::getAktivitetId)
                .map(Long::parseLong)
                .map(appService::hentAktivitet)
                .map(AktivitetWSMapper::mapTilAktivitet)
                .map(ResponseMapper::maptTilHentAktivitetResponse)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public HentAktivitetVersjonerResponse hentAktivitetVersjoner(HentAktivitetVersjonerRequest hentAktivitetVersjonerRequest) {
        return Optional.of(hentAktivitetVersjonerRequest.getAktivitetId())
                .map(Long::parseLong)
                .map(appService::hentAktivitetVersjoner)
                .map(aktivitetList -> aktivitetList
                        .stream()
                        .filter(this::erSynligForEksterne)
                        .map(AktivitetWSMapper::mapTilAktivitet)
                        .collect(Collectors.toList())
                ).map(ResponseMapper::mapTilOpprettNyAktivitetResponse)
                .orElseThrow(RuntimeException::new);
    }

    private boolean erSynligForEksterne(AktivitetData aktivitetData) {
        return !(kanHaInterneForandringer(aktivitetData) && erReferatetEndretForDetErPublisert(aktivitetData));

    }

    private boolean kanHaInterneForandringer(AktivitetData aktivitetData) {
        return aktivitetData.getAktivitetType() == AktivitetTypeData.MOTE ||
                aktivitetData.getAktivitetType() == AktivitetTypeData.SAMTALEREFERAT;
    }

    private boolean erReferatetEndretForDetErPublisert(AktivitetData aktivitetData) {
        val referatEndret = aktivitetData.getTransaksjonsType() == AktivitetTransaksjonsType.REFERAT_ENDRET ||
                aktivitetData.getTransaksjonsType() == AktivitetTransaksjonsType.REFERAT_OPPRETTET;
        return !aktivitetData.getMoteData().isReferatPublisert() && referatEndret;
    }

    @Override
    public OpprettNyAktivitetResponse opprettNyAktivitet(OpprettNyAktivitetRequest opprettNyAktivitetRequest) {

        val maybeAktivitet = Optional.of(opprettNyAktivitetRequest)
                .map(OpprettNyAktivitetRequest::getAktivitet);

        val fnr = maybeAktivitet
                .map(Aktivitet::getPersonIdent)
                .orElseThrow(RuntimeException::new);

        return maybeAktivitet
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(aktivitet -> appService.opprettNyAktivtet(fnr, aktivitet))
                .map(aktivitetData -> mapTilAktivitet(fnr, aktivitetData))
                .map(ResponseMapper::mapTilOpprettNyAktivitetResponse)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public EndreAktivitetStatusResponse endreAktivitetStatus(EndreAktivitetStatusRequest endreAktivitetStatusRequest) {
        return Optional.of(endreAktivitetStatusRequest)
                .map(EndreAktivitetStatusRequest::getAktivitet)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(appService::oppdaterStatus)
                .map(AktivitetWSMapper::mapTilAktivitet)
                .map(ResponseMapper::mapTilEndreAktivitetStatusResponse)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public EndreAktivitetEtikettResponse endreAktivitetEtikett(EndreAktivitetEtikettRequest endreAktivitetEtikettRequest) {
        return Optional.of(endreAktivitetEtikettRequest)
                .map(EndreAktivitetEtikettRequest::getAktivitet)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(appService::oppdaterEtikett)
                .map(AktivitetWSMapper::mapTilAktivitet)
                .map(ResponseMapper::mapTilEndreAktivitetEtikettResponse)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public EndreAktivitetResponse endreAktivitet(EndreAktivitetRequest endreAktivitetRequest) {
        return Optional.of(endreAktivitetRequest)
                .map(EndreAktivitetRequest::getAktivitet)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(appService::oppdaterAktivitet)
                .map(AktivitetWSMapper::mapTilAktivitet)
                .map(ResponseMapper::mapTilEndreAktivitetResponse)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public SlettAktivitetResponse slettAktivitet(SlettAktivitetRequest slettAktivitetRequest) {
        Optional.of(slettAktivitetRequest)
                .map(SlettAktivitetRequest::getAktivitetId)
                .map(Long::parseLong)
                .ifPresent(appService::slettAktivitet);
        return new SlettAktivitetResponse();
    }

    @Override
    public void ping() {
    }
}

