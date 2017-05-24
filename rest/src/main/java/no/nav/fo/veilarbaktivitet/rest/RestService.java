package no.nav.fo.veilarbaktivitet.rest;

import lombok.val;
import no.nav.fo.veilarbaktivitet.api.AktivitetController;
import no.nav.fo.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetsplanDTO;
import no.nav.fo.veilarbaktivitet.domain.EndringsloggDTO;
import no.nav.fo.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.fo.veilarbaktivitet.service.AppService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Component
public class RestService implements AktivitetController {

    @Inject
    private AppService appService;

    @Inject
    private Provider<HttpServletRequest> requestProvider;

    @Override
    public AktivitetsplanDTO hentAktivitetsplan() {
        val aktiviter = appService.hentAktiviteterForIdent(getUserIdent())
                .stream()
                .map(RestMapper::mapTilAktivitetDTO)
                .collect(Collectors.toList());

        return new AktivitetsplanDTO().setAktiviteter(aktiviter);
    }

    @Override
    public List<ArenaAktivitetDTO> hentArenaAktiviteter() {
        return Optional.of(appService.hentArenaAktiviteter(getUserIdent()))
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public AktivitetDTO opprettNyAktivitet(AktivitetDTO aktivitet) {
        return Optional.of(aktivitet)
                .map(RestMapper::mapTilAktivitetData)
                .map((aktivitetData) -> appService.opprettNyAktivtet(getUserIdent(), aktivitetData))
                .map(RestMapper::mapTilAktivitetDTO)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public AktivitetDTO oppdaterAktiviet(AktivitetDTO aktivitet) {
        return Optional.of(aktivitet)
                .map(RestMapper::mapTilAktivitetData)
                .map((aktivitetData) -> {
                    val orignalAktivitet = appService.hentAktivitet(Long.parseLong(aktivitet.getId()));
                    if (orignalAktivitet.isAvtalt()) {
                        orignalAktivitet.setTilDato(aktivitet.getTilDato());
                        //TODO: maybe extract
                        if (orignalAktivitet.getAktivitetType() == AktivitetTypeData.JOBBSOEKING) {
                            orignalAktivitet.setStillingsSoekAktivitetData(
                                    orignalAktivitet
                                            .getStillingsSoekAktivitetData()
                                            .setStillingsoekEtikett(
                                                    aktivitetData
                                                            .getStillingsSoekAktivitetData()
                                                            .getStillingsoekEtikett())
                            );
                        }

                        return appService.oppdaterAktivitet(orignalAktivitet);
                    }
                    return appService.oppdaterAktivitet(aktivitetData);
                })
                .map(RestMapper::mapTilAktivitetDTO)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public AktivitetDTO hentAktivitet(String aktivitetId) {
        return Optional.of(appService.hentAktivitet(Long.parseLong(aktivitetId)))
                .map(RestMapper::mapTilAktivitetDTO)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public void slettAktivitet(String aktivitetId) {
        appService.slettAktivitet(Long.parseLong(aktivitetId));
    }

    @Override
    public AktivitetDTO oppdaterStatus(AktivitetDTO aktivitet) {
        return Optional.of(aktivitet)
                .map(RestMapper::mapTilAktivitetData)
                .map((aktivitetData) -> appService.oppdaterStatus(aktivitetData))
                .map(RestMapper::mapTilAktivitetDTO)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public List<EndringsloggDTO> hentEndringsLoggForAktivitetId(String aktivitetId) {
        return Optional.of(aktivitetId)
                .map(Long::parseLong)
                .map(aId -> appService.hentEndringsloggForAktivitetId(aId))
                .map((endringslist) -> endringslist.stream()
                        .map(RestMapper::mapEndringsLoggDTO)
                        .collect(Collectors.toList())
                ).orElseThrow(RuntimeException::new);
    }

    private String getUserIdent() {
        return Optional.ofNullable(requestProvider.get().getParameter("fnr"))
                .orElseThrow(RuntimeException::new); // Hvordan håndere dette?
    }
}
