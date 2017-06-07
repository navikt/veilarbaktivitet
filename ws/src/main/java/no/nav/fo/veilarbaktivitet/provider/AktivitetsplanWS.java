package no.nav.fo.veilarbaktivitet.provider;

import lombok.val;
import no.nav.apiapp.soap.SoapTjeneste;
import no.nav.fo.veilarbaktivitet.mappers.AktivitetDataMapper;
import no.nav.fo.veilarbaktivitet.mappers.ArenaAktivitetWSMapper;
import no.nav.fo.veilarbaktivitet.mappers.ResponseMapper;
import no.nav.fo.veilarbaktivitet.service.AktivitetWSAppService;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.binding.BehandleAktivitetsplanV1;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.binding.HentAktivitetsplanSikkerhetsbegrensing;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Aktivitetsplan;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.*;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.of;
import static no.nav.fo.veilarbaktivitet.mappers.AktivitetDataMapper.mapTilAktivitetData;
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
    public OpprettNyAktivitetResponse opprettNyAktivitet(OpprettNyAktivitetRequest opprettNyAktivitetRequest) {

        return Optional.of(opprettNyAktivitetRequest)
                .map(OpprettNyAktivitetRequest::getAktivitet)
                .map((aktivitet) -> {
                    val aktivitetData = mapTilAktivitetData(aktivitet);
                    val lagretAktivtet = appService.opprettNyAktivtet(aktivitet.getPersonIdent(), aktivitetData);
                    return mapTilAktivitet(aktivitet.getPersonIdent(), lagretAktivtet);
                })
                .map(ResponseMapper::mapTilOpprettNyAktivitetResponse)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public HentArenaAktiviteterResponse hentArenaAktiviteter(HentArenaAktiviteterRequest hentArenaAktiviteterRequest) {
        return Optional.of(appService.hentArenaAktiviteter(hentArenaAktiviteterRequest.getPersonident()))
                .map(arenaAktiviterDTO -> {
                    val res = new HentArenaAktiviteterResponse();
                    res.getArenaaktiviteter().addAll(
                            arenaAktiviterDTO.stream()
                                    .map(ArenaAktivitetWSMapper::mapTilArenaAktivitet)
                                    .collect(Collectors.toList()));
                    return res;
                })
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public HentAktivitetResponse hentAktivitet(HentAktivitetRequest hentAktivitetRequest) {
        return Optional.of(hentAktivitetRequest)
                .map(HentAktivitetRequest::getAktivitetId)
                .map(Long::parseLong)
                .map(appService::hentAktivitet)
                .map(aktivitet -> mapTilAktivitet("", aktivitet))
                .map(aktivitet -> {
                    val res = new HentAktivitetResponse();
                    res.setAktivitet(aktivitet);
                    return res;
                }).orElseThrow(RuntimeException::new);
    }

    @Override
    public HentAktivitetsplanResponse hentAktivitetsplan(HentAktivitetsplanRequest hentAktivitetsplanRequest) throws HentAktivitetsplanSikkerhetsbegrensing {
        val wsHentAktiviteterResponse = new HentAktivitetsplanResponse();
        val aktivitetsplan = new Aktivitetsplan();
        wsHentAktiviteterResponse.setAktivitetsplan(aktivitetsplan);

        appService.hentAktiviteterForIdent(hentAktivitetsplanRequest.getPersonident())
                .stream()
                .map(aktivitet -> mapTilAktivitet(hentAktivitetsplanRequest.getPersonident(), aktivitet))
                .forEach(aktivitetsplan.getAktivitetListe()::add);

        return wsHentAktiviteterResponse;
    }

    @Override
    public HentEndringsLoggForAktivitetResponse hentEndringsLoggForAktivitet(HentEndringsLoggForAktivitetRequest hentEndringsLoggForAktivitetRequest) {
//        val endringsloggResponse = new HentEndringsLoggForAktivitetResponse();
//        val endringer = endringsloggResponse.getEndringslogg();
//
//        of(hentEndringsLoggForAktivitetRequest)
//                .map(HentEndringsLoggForAktivitetRequest::getAktivitetId)
//                .map(Long::parseLong)
//                .map(aktivietId -> appService.hentEndringsloggForAktivitetId(aktivietId))
//                .ifPresent((endringslist) -> endringslist.stream()
//                        .map(SoapMapper::somEndringsLoggResponse)
//                        .forEach(endringer::add)
//                );
//        return endringsloggResponse;
        return null;
    }

    @Override
    public EndreAktivitetStatusResponse endreAktivitetStatus(EndreAktivitetStatusRequest endreAktivitetStatusRequest) {
        return Optional.of(endreAktivitetStatusRequest)
                .map(EndreAktivitetStatusRequest::getAktivitet)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(appService::oppdaterStatus)
                .map(aktivitet -> mapTilAktivitet("", aktivitet))
                .map(ResponseMapper::mapTilEndreAktivitetStatusResponse)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public EndreAktivitetEtikettResponse endreAktivitetEtikett(EndreAktivitetEtikettRequest endreAktivitetEtikettRequest) {
        return Optional.of(endreAktivitetEtikettRequest)
                .map(EndreAktivitetEtikettRequest::getAktivitet)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(appService::oppdaterEtikett)
                .map(aktivitet -> mapTilAktivitet("", aktivitet))
                .map(ResponseMapper::mapTilEndreAktivitetEtikettResponse)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public EndreAktivitetResponse endreAktivitet(EndreAktivitetRequest endreAktivitetRequest) {
        return Optional.of(endreAktivitetRequest)
                .map(EndreAktivitetRequest::getAktivitet)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(appService::oppdaterAktivitet)
                .map(aktivtet -> mapTilAktivitet("", aktivtet))
                .map(ResponseMapper::mapTilEndreAktivitetResponse)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public SlettAktivitetResponse slettAktivitet(SlettAktivitetRequest slettAktivitetRequest) {
        of(slettAktivitetRequest)
                .map(SlettAktivitetRequest::getAktivitetId)
                .map(Long::parseLong)
                .ifPresent(appService::slettAktivitet);
        return new SlettAktivitetResponse();
    }

    @Override
    public void ping() {}
}

