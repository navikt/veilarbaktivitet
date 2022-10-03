package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static no.nav.veilarbaktivitet.util.DateUtils.dateToLocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AktivitetskortService {

    private final AktivitetService aktivitetService;
    private final AktivitetDAO aktivitetDAO;
    private final AktivitetsMessageDao aktivitetsMessageDao;

    private final PersonService personService;

    public void upsertAktivitetskort(TiltaksaktivitetDTO tiltaksaktivitet) {
        Optional<AktivitetData> maybeAktivitet = aktivitetDAO.hentAktivitetByFunksjonellId(tiltaksaktivitet.id);
        Person.AktorId aktorIdForPersonBruker = personService.getAktorIdForPersonBruker(Person.fnr(tiltaksaktivitet.personIdent)).orElseThrow();

        var aktivitetData = maybeAktivitet
                .map(gammelaktivitet -> AktivitetskortMapper.mapTilAktivitetData(tiltaksaktivitet, dateToLocalDateTime(gammelaktivitet.getOpprettetDato()), tiltaksaktivitet.endretDato, aktorIdForPersonBruker.get()))
                .orElse(AktivitetskortMapper.mapTilAktivitetData(tiltaksaktivitet, tiltaksaktivitet.endretDato, tiltaksaktivitet.endretDato, aktorIdForPersonBruker.get()));

        Person.NavIdent endretAvIdent = Person.navIdent(aktivitetData.getEndretAv());

        maybeAktivitet
            .ifPresentOrElse(
                (gammelAktivitet) -> oppdaterTiltaksAktivitet(gammelAktivitet, aktivitetData, endretAvIdent),
                () -> opprettTiltaksAktivitet(aktivitetData, endretAvIdent, tiltaksaktivitet.endretDato)
            );
    }

    private void oppdaterTiltaksAktivitet(AktivitetData gammelAktivitet, AktivitetData nyAktivitet, Person endretAvIdent) {
        if (!gammelAktivitet.endringTillatt()) {
            // TODO: Publish error to dead-letter-queue
            return;
        };
        var lol = Stream.of(gammelAktivitet)
            .map((aktivitet) -> {
                if (AktivitetskortCompareUtil.erFaktiskOppdatert(nyAktivitet, aktivitet)) {
                    return aktivitetService.oppdaterAktivitet(aktivitet, nyAktivitet, endretAvIdent);
                } else {
                    return aktivitet;
                }
            })
            .map((aktivitet) -> {
                if (aktivitet.getStatus() != nyAktivitet.getStatus() && aktivitet.endringTillatt()) {
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

    private void opprettTiltaksAktivitet(AktivitetData aktivitetData, Person endretAvIdent, LocalDateTime opprettet) {
        aktivitetService.opprettAktivitet(Person.aktorId(aktivitetData.getAktorId()), aktivitetData, endretAvIdent, opprettet);
    }

    public boolean harSettMelding(UUID messageId) {
        return aktivitetsMessageDao.exist(messageId);
    }

    public void lagreMeldingsId(UUID messageId, UUID funksjonellId) {
        aktivitetsMessageDao.insert(messageId, funksjonellId);
    }

}
