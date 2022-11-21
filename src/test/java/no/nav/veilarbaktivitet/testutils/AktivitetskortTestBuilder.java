package no.nav.veilarbaktivitet.testutils;

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitetskort.*;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import static no.nav.veilarbaktivitet.aktivitetskort.AktivitetsbestillingCreator.ARENA_TILTAK_AKTIVITET_ACL;
import static no.nav.veilarbaktivitet.aktivitetskort.IdentType.ARENAIDENT;

public class AktivitetskortTestBuilder {

    public static Aktivitetskort ny(UUID funksjonellId, AktivitetStatus aktivitetStatus, ZonedDateTime endretTidspunkt, MockBruker mockBruker) {
            return Aktivitetskort.builder()
                    .id(funksjonellId)
                    .personIdent(mockBruker.getFnr())
                    .startDato(LocalDate.now().minusDays(30))
                    .sluttDato(LocalDate.now().minusDays(30))
                    .tittel("The Elder Scrolls: Arena")
                    .beskrivelse("arenabeskrivelse")
                    .aktivitetStatus(aktivitetStatus)
                    .avtaltMedNav(true)
                    .endretAv(new IdentDTO("arenaEndretav", ARENAIDENT))
                    .endretTidspunkt(endretTidspunkt)
                    .etikett(new Etikett("SOKT_INN"))
                    .detalj(new Attributt("arrangørnavn", "Arendal"))
                    .detalj(new Attributt("deltakelsesprosent", "40%"))
                    .detalj(new Attributt("dager per uke", "2"))
                    .build();
    }

    public static KafkaAktivitetskortWrapperDTO aktivitetskortMelding(Aktivitetskort payload, UUID messageId, String source, AktivitetskortType aktivitetskortType) {
        return KafkaAktivitetskortWrapperDTO.builder()
                .messageId(messageId)
                .source(source)
                .actionType(ActionType.UPSERT_AKTIVITETSKORT_V1)
                .aktivitetskort(payload)
                .aktivitetskortType(aktivitetskortType)
                .build();
    }

    public static KafkaAktivitetskortWrapperDTO aktivitetskortMelding(Aktivitetskort payload) {
        return AktivitetskortTestBuilder.aktivitetskortMelding(payload, UUID.randomUUID(), ARENA_TILTAK_AKTIVITET_ACL, AktivitetskortType.ARENA_TILTAK);
    }
}

