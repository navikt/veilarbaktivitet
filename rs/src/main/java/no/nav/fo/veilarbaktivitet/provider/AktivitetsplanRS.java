package no.nav.fo.veilarbaktivitet.provider;

import lombok.val;
import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarbaktivitet.api.AktivitetController;
import no.nav.fo.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetsplanDTO;
import no.nav.fo.veilarbaktivitet.domain.Person;
import no.nav.fo.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.fo.veilarbaktivitet.mappers.AktivitetDTOMapper;
import no.nav.fo.veilarbaktivitet.mappers.AktivitetDataMapper;
import no.nav.fo.veilarbaktivitet.service.AktivitetRSAppService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Component
public class AktivitetsplanRS implements AktivitetController {

    private final AktivitetRSAppService appService;
    private final PepClient pepClient;
    private final Provider<HttpServletRequest> requestProvider;

    @Inject
    public AktivitetsplanRS(
            AktivitetRSAppService appService,
            PepClient pepClient,
            Provider<HttpServletRequest> requestProvider
    ) {
        this.appService = appService;
        this.pepClient = pepClient;
        this.requestProvider = requestProvider;
    }

    @Override
    public AktivitetsplanDTO hentAktivitetsplan() {
        val aktiviter = appService
                .hentAktiviteterForIdent(getUserIdent())
                .stream()
                .map(AktivitetDTOMapper::mapTilAktivitetDTO)
                .collect(Collectors.toList());

        return new AktivitetsplanDTO().setAktiviteter(aktiviter);
    }

    @Override
    public AktivitetDTO hentAktivitet(String aktivitetId) {
        return Optional.of(appService.hentAktivitet(Long.parseLong(aktivitetId)))
                .map(AktivitetDTOMapper::mapTilAktivitetDTO)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public List<ArenaAktivitetDTO> hentArenaAktiviteter() {
        return getFnr()
                .map(appService::hentArenaAktiviteter)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public AktivitetDTO opprettNyAktivitet(AktivitetDTO aktivitet) {
        return Optional.of(aktivitet)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map((aktivitetData) -> appService.opprettNyAktivtet(getUserIdent(), aktivitetData))
                .map(AktivitetDTOMapper::mapTilAktivitetDTO)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public AktivitetDTO oppdaterAktiviet(AktivitetDTO aktivitet) {
        return Optional.of(aktivitet)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(appService::oppdaterAktivitet)
                .map(AktivitetDTOMapper::mapTilAktivitetDTO)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public AktivitetDTO oppdaterEtikett(AktivitetDTO aktivitet) {
        return Optional.of(aktivitet)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(appService::oppdaterEtikett)
                .map(AktivitetDTOMapper::mapTilAktivitetDTO)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public void slettAktivitet(String aktivitetId) {
        appService.slettAktivitet(Long.parseLong(aktivitetId));
    }

    @Override
    public AktivitetDTO oppdaterStatus(AktivitetDTO aktivitet) {
        return Optional.of(aktivitet)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(appService::oppdaterStatus)
                .map(AktivitetDTOMapper::mapTilAktivitetDTO)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public List<AktivitetDTO> hentAktivitetVersjoner(String aktivitetId) {
        return Optional.of(aktivitetId)
                .map(Long::parseLong)
                .map(appService::hentAktivitetVersjoner)
                .map(aktivitetList -> aktivitetList
                        .stream()
                        .map(AktivitetDTOMapper::mapTilAktivitetDTO)
                        .collect(Collectors.toList())
                ).orElseThrow(RuntimeException::new);
    }

    @Path("{aktivitetId}/referat")
    public ReferatRessurs referatRessurs(@PathParam("aktivitetId") long aktivitetId) {
        return new ReferatRessurs(aktivitetId, appService);
    }

    private Person getUserIdent() {
        Optional<Person> fnr = Optional.ofNullable(requestProvider.get().getParameter("fnr")).map(Person::fnr);
        Optional<Person> aktorId = Optional.ofNullable(requestProvider.get().getParameter("aktorId")).map(Person::aktorId);

        return fnr.orElseGet(() -> aktorId.orElseThrow(RuntimeException::new));
    }

    private Optional<Person.Fnr> getFnr() {
        return Optional.of(getUserIdent())
                .filter((person) -> person instanceof Person.Fnr)
                .map((person) -> (Person.Fnr)person);
    }
}
