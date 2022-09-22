package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AktivitetskortService {

    private final AktivitetService aktivitetService;
    private final AktivitetDAO aktivitetDAO;

    public void upsertAktivitetskort(AktivitetskortDTO aktivitetskortDTO) {
        AktivitetData aktivitetData = AktivitetskortMapper.map(aktivitetskortDTO.actionType, aktivitetskortDTO.payload);

        Optional<AktivitetData> maybeAktivitet = Optional.of(aktivitetData.getFunksjonellId())
                .map(aktivitetDAO::hentAktivitetByFunksjonellId);

        if (maybeAktivitet.isPresent()) { // TODO legg til endretAv felt i acl
            aktivitetService.oppdaterAktivitet(maybeAktivitet.get(), aktivitetData, Person.navIdent("test"));
        } else {
            aktivitetService.opprettAktivitet(Person.aktorId(aktivitetData.getAktorId()), aktivitetData, Person.navIdent("test"));
        }
    }
}
