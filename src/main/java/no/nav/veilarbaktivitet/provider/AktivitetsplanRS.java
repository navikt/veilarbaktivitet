package no.nav.veilarbaktivitet.provider;

import lombok.val;
import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.feil.FeilType;
import no.nav.common.auth.SubjectHandler;
import no.nav.veilarbaktivitet.api.AktivitetController;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetsplanDTO;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.mappers.AktivitetDataMapper;
import no.nav.veilarbaktivitet.service.AktivitetAppService;
import no.nav.veilarbaktivitet.service.BrukerService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Component
public class AktivitetsplanRS implements AktivitetController {

    private final AktivitetAppService appService;
    private final Provider<HttpServletRequest> requestProvider;

    @Inject
    public AktivitetsplanRS(
            AktivitetAppService appService,
            Provider<HttpServletRequest> requestProvider
    ) {
        this.appService = appService;
        this.requestProvider = requestProvider;
    }

    @Override
    public AktivitetsplanDTO hentAktivitetsplan() {
        val aktiviter = appService
                .hentAktiviteterForIdent(getContextUserIdent())
                .stream()
                .map(AktivitetDTOMapper::mapTilAktivitetDTO)
                .collect(Collectors.toList());

        return new AktivitetsplanDTO().setAktiviteter(aktiviter);
    }

    @Override
    public AktivitetDTO hentAktivitet(String aktivitetId) {
        return Optional.of(appService.hentAktivitet(Long.parseLong(aktivitetId)))
                .map(AktivitetDTOMapper::mapTilAktivitetDTO)
                .orElseThrow(() -> new Feil(FeilType.FINNES_IKKE));
    }

    @Override
    public List<ArenaAktivitetDTO> hentArenaAktiviteter() {
        return getFnr()
                .map(appService::hentArenaAktiviteter)
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
                ).orElseThrow(() -> new Feil(FeilType.FINNES_IKKE));
    }

    @Override
    public AktivitetDTO opprettNyAktivitet(AktivitetDTO aktivitet, boolean automatiskOpprettet) {
        return Optional.of(aktivitet)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(aktivitetData -> aktivitetData.withAutomatiskOpprettet(automatiskOpprettet))
                .map((aktivitetData) -> appService.opprettNyAktivitet(getContextUserIdent(), aktivitetData))
                .map(AktivitetDTOMapper::mapTilAktivitetDTO)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public AktivitetDTO oppdaterAktivitet(AktivitetDTO aktivitet) {
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

    @Path("{aktivitetId}/referat")
    public ReferatRessurs referatRessurs() {
        return new ReferatRessurs(appService);
    }

    private Person getContextUserIdent() {
        if (BrukerService.erEksternBruker()) {
            return SubjectHandler.getIdent().map(Person::fnr).orElseThrow(RuntimeException::new);
        }

        Optional<Person> fnr = Optional.ofNullable(requestProvider.get().getParameter("fnr")).map(Person::fnr);
        Optional<Person> aktorId = Optional.ofNullable(requestProvider.get().getParameter("aktorId")).map(Person::aktorId);
        return fnr.orElseGet(() -> aktorId.orElseThrow(RuntimeException::new));
    }

    private Optional<Person.Fnr> getFnr() {
        return Optional.of(getContextUserIdent())
                .filter((person) -> person instanceof Person.Fnr)
                .map((person) -> (Person.Fnr)person);
    }
}
