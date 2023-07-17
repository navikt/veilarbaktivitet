package no.nav.veilarbaktivitet.testutils;

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitetskort.ActionType;
import no.nav.veilarbaktivitet.aktivitetskort.Aktivitetskort;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortType;
import no.nav.veilarbaktivitet.aktivitetskort.dto.KafkaAktivitetskortWrapperDTO;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Attributt;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Etikett;
import no.nav.veilarbaktivitet.aktivitet.domain.Ident;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.person.Innsender;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static no.nav.veilarbaktivitet.aktivitetskort.AktivitetsbestillingCreator.ARENA_TILTAK_AKTIVITET_ACL;


public class AktivitetskortTestBuilder {

    public static Aktivitetskort ny(UUID funksjonellId, AktivitetStatus aktivitetStatus, ZonedDateTime endretTidspunkt, MockBruker mockBruker) {
        return new Aktivitetskort(
               funksjonellId,
               mockBruker.getFnr(),
                "The Elder Scrolls: Arena",
                "arenabeskrivelse",
                aktivitetStatus,
                LocalDate.now().minusDays(30),
                LocalDate.now().minusDays(30),
                new Ident("arenaEndretav", Innsender.ARENAIDENT),
                endretTidspunkt,
                true,
               null,
                null,
                List.of(
                    new Attributt("arrangørnavn", "Arendal"),
                    new Attributt("deltakelsesprosent", "40%"),
                    new Attributt("dager per uke", "2")
                ),
               List.of(new Etikett("SOKT_INN"))
        );
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

