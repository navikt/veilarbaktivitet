package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AktivitetskortService {

    private final AktivitetService aktivitetService;
    private final AktivitetDAO aktivitetDAO;

    private final PersonService personService;

    public void upsertAktivitetskort(KafkaAktivitetWrapperDTO kafkaAktivitetWrapperDTO) {
        Person.AktorId aktorIdForPersonBruker = personService.getAktorIdForPersonBruker(Person.fnr(kafkaAktivitetWrapperDTO.payload.get("personIdent").asText())).orElseThrow();
        AktivitetData aktivitetData = AktivitetskortMapper.map(kafkaAktivitetWrapperDTO.actionType, kafkaAktivitetWrapperDTO.payload, aktorIdForPersonBruker.get());
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
        if (AktivitetskortCompareUtil.erFaktiskOppdatert(gammelAktivitet, nyAktivitet)) {
            aktivitetService.oppdaterAktivitet(gammelAktivitet, nyAktivitet, endretAvIdent);
        }

        if (gammelAktivitet.getStatus() != nyAktivitet.getStatus()) {
            aktivitetService.oppdaterStatus(
                gammelAktivitet,
                nyAktivitet,
                Person.navIdent(nyAktivitet.getEndretAv())
            );
        }
    }

    private void opprettTiltaksAktivitet(AktivitetData aktivitetData, Person endretAvIdent) {
        aktivitetService.opprettAktivitet(Person.aktorId(aktivitetData.getAktorId()), aktivitetData, endretAvIdent);
    }
}
