package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AktivitetskortService {

    private final AktivitetService aktivitetService;
    private final AktivitetDAO aktivitetDAO;

    private final PersonService personService;

    public void upsertAktivitetskort(TiltaksaktivitetDTO tiltaksaktivitet) {
        Person.AktorId aktorIdForPersonBruker = personService.getAktorIdForPersonBruker(Person.fnr(tiltaksaktivitet.personIdent)).orElseThrow();
        AktivitetData aktivitetData = AktivitetskortMapper.mapTilAktivitetData(tiltaksaktivitet, aktorIdForPersonBruker.get());
        Optional<AktivitetData> maybeAktivitet = Optional.ofNullable(aktivitetData.getFunksjonellId())
                .flatMap(aktivitetDAO::hentAktivitetByFunksjonellId);

        Person.NavIdent endretAvIdent = Person.navIdent(aktivitetData.getEndretAv());

        maybeAktivitet
            .ifPresentOrElse(
                (gammelAktivitet) -> oppdaterTiltaksAktivitet(gammelAktivitet, aktivitetData, endretAvIdent),
                () -> opprettTiltaksAktivitet(aktivitetData, endretAvIdent)
            );
    }

    private void oppdaterTiltaksAktivitet(AktivitetData gammelAktivitet, AktivitetData nyAktivitet, Person endretAvIdent) {
        var lol = Stream.of(gammelAktivitet)
            .map((aktivitet) -> {
                if (AktivitetskortCompareUtil.erFaktiskOppdatert(aktivitet, nyAktivitet)) {
                    return aktivitetService.oppdaterAktivitet(aktivitet, nyAktivitet, endretAvIdent);
                } else {
                    return aktivitet;
                }
            })
            .map((aktivitet) -> {
                if (aktivitet.getStatus() != nyAktivitet.getStatus()) {
                    return aktivitetService.oppdaterStatus(
                        aktivitet,
                        nyAktivitet, // TODO: Populer avbrutt-tekstfelt
                       Person.navIdent(nyAktivitet.getEndretAv())
                    );
                } else {
                    return aktivitet;
                }
            }).findFirst();
        log.info(lol.get().toString());
    }

    private void opprettTiltaksAktivitet(AktivitetData aktivitetData, Person endretAvIdent) {
        aktivitetService.opprettAktivitet(Person.aktorId(aktivitetData.getAktorId()), aktivitetData, endretAvIdent);
    }
}
