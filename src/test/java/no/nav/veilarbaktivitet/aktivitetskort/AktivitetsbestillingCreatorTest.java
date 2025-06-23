package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.SneakyThrows;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.AktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.EksternAktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.KasseringsBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortStatus;
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.*;
import no.nav.veilarbaktivitet.aktivitetskort.feil.KeyErIkkeFunksjonellIdFeil;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static no.nav.veilarbaktivitet.aktivitetskort.AktivitetsbestillingCreator.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AktivitetsbestillingCreatorTest {
    AktivitetsbestillingCreator aktivitetsbestillingCreator;

    @BeforeEach
    public void setup() {
        PersonService personService = Mockito.mock(PersonService.class);
        Mockito.when(personService.getAktorIdForPersonBruker(ArgumentMatchers.any(Person.class))).thenReturn(Optional.of(Person.aktorId("12345678901")));
        aktivitetsbestillingCreator = new AktivitetsbestillingCreator(personService);
    }

    @Test
    void should_have_correct_timezone_when_serializing() {
        var jsonNode = AktivitetskortProducerUtil.validExampleAktivitetskortRecord(Person.fnr("1234567890"));
        var endretTidspunkt = jsonNode.path("aktivitetskort").get("endretTidspunkt").asText();
        assertEquals("2022-01-01T00:00:00.001+01:00", endretTidspunkt);
    }

    @Test
    @SneakyThrows
    void should_handle_zoned_datetime_format() {
        String json = AktivitetskortProducerUtil.exampleFromFile("validaktivitetskortZonedDatetime.json");
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "56155242-6481-43b5-9eac-4d7af695bf9d", json);
        AktivitetskortBestilling aktivitetskortBestilling = (AktivitetskortBestilling) aktivitetsbestillingCreator.lagBestilling(consumerRecord, UUID.randomUUID());
        ZonedDateTime expected = ZonedDateTime.of(2022, 10, 19, 12, 0, 0, 0, ZoneId.of("UTC"));
        assertThat(aktivitetskortBestilling.getAktivitetskort().getEndretTidspunkt()).isCloseTo(expected, Assertions.within(100, ChronoUnit.MILLIS));
    }

    @Test
    @SneakyThrows
    void should_handle_serialize_action_type() {
        String json = AktivitetskortProducerUtil.exampleFromFile("validaktivitetskortZonedDatetime.json");
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "56155242-6481-43b5-9eac-4d7af695bf9d", json);
        AktivitetskortBestilling aktivitetskortBestilling = (AktivitetskortBestilling) aktivitetsbestillingCreator.lagBestilling(consumerRecord, UUID.randomUUID());
        assertThat(aktivitetskortBestilling.getMessageId()).isEqualTo(UUID.fromString("2edf9ba0-b195-49ff-a5cd-939c7f26826f"));
        assertThat(aktivitetskortBestilling.getSource()).isEqualTo("TEAM_TILTAK");
        assertThat(aktivitetskortBestilling.getActionType()).isEqualTo(ActionType.UPSERT_AKTIVITETSKORT_V1);

    }

    @Test
    @SneakyThrows
    void should_throw_exception_when_kafka_key_is_not_equal_to_funksjonell_id() {
        String json = AktivitetskortProducerUtil.exampleFromFile("validaktivitetskortZonedDatetime.json");
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "invalid-key", json);
        assertThrows(KeyErIkkeFunksjonellIdFeil.class, () -> aktivitetsbestillingCreator.lagBestilling(consumerRecord, UUID.randomUUID()));
    }

    @Test
    @SneakyThrows
    void should_handle_zoned_datetime_format_pluss_time() {
        String json = AktivitetskortProducerUtil.exampleFromFile("validaktivitetskortZonedDatetime+Time.json");
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "56155242-6481-43b5-9eac-4d7af695bf9d", json);
        AktivitetskortBestilling aktivitetskortBestilling = (AktivitetskortBestilling) aktivitetsbestillingCreator.lagBestilling(consumerRecord, UUID.randomUUID());
        ZonedDateTime expected = ZonedDateTime.of(2022, 10, 19, 11, 0, 0, 0, ZoneId.of("UTC"));
        assertThat(aktivitetskortBestilling.getAktivitetskort().getEndretTidspunkt()).isCloseTo(expected, Assertions.within(100, ChronoUnit.MILLIS));

    }

    @Test
    @SneakyThrows
    void should_handle_UNzoned_datetime_format() {
        String json = AktivitetskortProducerUtil.exampleFromFile("validaktivitetskortUnzonedDatetime.json");
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "56155242-6481-43b5-9eac-4d7af695bf9d", json);
        var aktivitetskortBestilling = (AktivitetskortBestilling) aktivitetsbestillingCreator.lagBestilling(consumerRecord, UUID.randomUUID());
        ZonedDateTime expected = ZonedDateTime.of(2022, 10, 19, 12, 0, 0, 0, ZoneId.of("Europe/Oslo"));
        var endretTidspunkt = aktivitetskortBestilling.getAktivitetskort().getEndretTidspunkt();
        assertThat(endretTidspunkt).isCloseTo(expected, Assertions.within(100, ChronoUnit.MILLIS));
    }

    @Test
    @SneakyThrows
    void should_be_able_to_deserialize_kasserings_bestilling() {
        String json = AktivitetskortProducerUtil.exampleFromFile("validkassering.json");
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "56155242-6481-43b5-9eac-4d7af695bf9d", json);
        var aktivitetskortBestilling = aktivitetsbestillingCreator.lagBestilling(consumerRecord, UUID.randomUUID());
        assertThat(aktivitetskortBestilling).isInstanceOf(KasseringsBestilling.class);
    }


    @Test
    @SneakyThrows
    void should_handle_valid_aktivitetskort() {
        String json = AktivitetskortProducerUtil.exampleFromFile("validaktivitetskort.json");
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "56155242-6481-43b5-9eac-4d7af695bf9d", json);
        EksternAktivitetskortBestilling aktivitetskortBestilling = (EksternAktivitetskortBestilling)aktivitetsbestillingCreator.lagBestilling(consumerRecord, UUID.randomUUID()) ;
        assertThat(aktivitetskortBestilling).isInstanceOf(AktivitetskortBestilling.class);
        assertThat(aktivitetskortBestilling.getSource()).isEqualTo(MessageSource.TEAM_TILTAK.name());
        assertThat(aktivitetskortBestilling.getAktivitetskortType()).isEqualTo(AktivitetskortType.MENTOR);
        var aktivitetskort = aktivitetskortBestilling.getAktivitetskort();
        assertThat(aktivitetskort.getAktivitetStatus()).isEqualTo(AktivitetskortStatus.PLANLAGT);
        assertThat(aktivitetskort.getTittel()).isEqualTo("The Elder Scrolls");
        assertThat(aktivitetskort.getBeskrivelse()).isEqualTo("aktivitetsbeskrivelse");
        var oppgave = aktivitetskort.getOppgave();
        assertThat(oppgave.ekstern().url()).isEqualTo(new URL("http://localhost:8080/ekstern"));
        var handlinger = aktivitetskort.getHandlinger();
        assertThat(handlinger).contains(new LenkeSeksjon("tekst", "subtekst", new URL("http://localhost:8080/ekstern"), LenkeType.EKSTERN));
        var etiketter = aktivitetskort.getEtiketter();
        assertThat(etiketter).contains(new Etikett("Etikett tekst", Sentiment.NEUTRAL, "INNSOKT"));
    }

    @Test
    @SneakyThrows
    void should_handle_unknown_source_aktivitetskort() {
        String json = AktivitetskortProducerUtil.exampleFromFile("validaktivitetskortWithUnknownSource.json");
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "56155242-6481-43b5-9eac-4d7af695bf9d", json);
        EksternAktivitetskortBestilling aktivitetskortBestilling = (EksternAktivitetskortBestilling)aktivitetsbestillingCreator.lagBestilling(consumerRecord, UUID.randomUUID()) ;
        assertThat(aktivitetskortBestilling).isInstanceOf(AktivitetskortBestilling.class);
        assertThat(aktivitetskortBestilling.getSource()).isEqualTo("TEAM_UNKNOWN");
        assertThat(aktivitetskortBestilling.getAktivitetskortType()).isEqualTo(AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD);
        var aktivitetskort = aktivitetskortBestilling.getAktivitetskort();
        assertThat(aktivitetskort.getAktivitetStatus()).isEqualTo(AktivitetskortStatus.PLANLAGT);
        assertThat(aktivitetskort.getTittel()).isEqualTo("The Elder Scrolls");
        assertThat(aktivitetskort.getBeskrivelse()).isEqualTo("aktivitetsbeskrivelse");
        var oppgave = aktivitetskort.getOppgave();
        assertThat(oppgave.ekstern().url()).isEqualTo(new URL("http://localhost:8080/ekstern"));
        var handlinger = aktivitetskort.getHandlinger();
        assertThat(handlinger).contains(new LenkeSeksjon("tekst", "subtekst", new URL("http://localhost:8080/ekstern"), LenkeType.EKSTERN));
    }

    @Test
    @SneakyThrows
    void require_header_on_arena_tiltak() {
        String json = AktivitetskortProducerUtil.exampleFromFile("validArenaAktivitetskort.json");
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0L, "56155242-6481-43b5-9eac-4d7af695bf9d", json);
        var randomUUID = UUID.randomUUID();

        assertThrows(RuntimeException.class, () -> aktivitetsbestillingCreator.lagBestilling(consumerRecord, randomUUID));
        String arenaId = "ARENATA1234567";
        consumerRecord.headers().add(HEADER_EKSTERN_REFERANSE_ID, arenaId.getBytes());
        assertThrows(RuntimeException.class, () -> aktivitetsbestillingCreator.lagBestilling(consumerRecord, randomUUID));
        String tiltakskode = "VASV";
        consumerRecord.headers().add(HEADER_EKSTERN_ARENA_TILTAKSKODE, tiltakskode.getBytes());
        assertThrows(RuntimeException.class, () -> aktivitetsbestillingCreator.lagBestilling(consumerRecord, randomUUID));
        String oppfolgingsperiode = "278f090f-09cc-4720-8638-f68020f3b417";
        consumerRecord.headers().add(HEADER_OPPFOLGINGSPERIODE, oppfolgingsperiode.getBytes());

        ArenaAktivitetskortBestilling aktivitetskortBestilling = (ArenaAktivitetskortBestilling)aktivitetsbestillingCreator.lagBestilling(consumerRecord, randomUUID) ;
        assertThat(aktivitetskortBestilling).isInstanceOf(AktivitetskortBestilling.class);
        assertThat(aktivitetskortBestilling.getSource()).isEqualTo(MessageSource.ARENA_TILTAK_AKTIVITET_ACL.name());
        assertThat(aktivitetskortBestilling.getAktivitetskortType()).isEqualTo(AktivitetskortType.ARENA_TILTAK);
        assertThat(aktivitetskortBestilling.getOppfolgingsperiode()).isEqualTo(UUID.fromString(oppfolgingsperiode));
        assertThat(aktivitetskortBestilling.getEksternReferanseId()).isEqualTo(new ArenaId(arenaId));
        assertThat(aktivitetskortBestilling.getArenaTiltakskode()).isEqualTo(tiltakskode);
        var aktivitetskort = aktivitetskortBestilling.getAktivitetskort();
        assertThat(aktivitetskort.getAktivitetStatus()).isEqualTo(AktivitetskortStatus.PLANLAGT);
        assertThat(aktivitetskort.getTittel()).isEqualTo("The Elder Scrolls");
        assertThat(aktivitetskort.getBeskrivelse()).isEqualTo("aktivitetsbeskrivelse");
    }

    @Test
    @SneakyThrows
    void handle_oppfolgingsperiode_slutt_header() {
        String json = AktivitetskortProducerUtil.exampleFromFile("validArenaAktivitetskort.json");
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0L, "56155242-6481-43b5-9eac-4d7af695bf9d", json);

        String arenaId = "ARENATA1234567";
        consumerRecord.headers().add(HEADER_EKSTERN_REFERANSE_ID, arenaId.getBytes());
        String tiltakskode = "VASV";
        consumerRecord.headers().add(HEADER_EKSTERN_ARENA_TILTAKSKODE, tiltakskode.getBytes());
        String oppfolgingsperiode = "278f090f-09cc-4720-8638-f68020f3b417";
        consumerRecord.headers().add(HEADER_OPPFOLGINGSPERIODE, oppfolgingsperiode.getBytes());
        String oppfolgingsperiodeSlutt = "2023-01-01T10:00:00.000Z";
        consumerRecord.headers().add(HEADER_OPPFOLGINGSPERIODE_SLUTT, oppfolgingsperiodeSlutt.getBytes());

        ArenaAktivitetskortBestilling aktivitetskortBestilling = (ArenaAktivitetskortBestilling)aktivitetsbestillingCreator.lagBestilling(consumerRecord, UUID.randomUUID()) ;
        assertThat(aktivitetskortBestilling).isInstanceOf(AktivitetskortBestilling.class);
        assertThat(aktivitetskortBestilling.getSource()).isEqualTo(MessageSource.ARENA_TILTAK_AKTIVITET_ACL.name());
        assertThat(aktivitetskortBestilling.getAktivitetskortType()).isEqualTo(AktivitetskortType.ARENA_TILTAK);
        assertThat(aktivitetskortBestilling.getOppfolgingsperiode()).isEqualTo(UUID.fromString(oppfolgingsperiode));
        assertThat(aktivitetskortBestilling.getEksternReferanseId()).isEqualTo(new ArenaId(arenaId));
        assertThat(aktivitetskortBestilling.getOppfolgingsperiodeSlutt()).isEqualTo(oppfolgingsperiodeSlutt);
        assertThat(aktivitetskortBestilling.getArenaTiltakskode()).isEqualTo(tiltakskode);
        var aktivitetskort = aktivitetskortBestilling.getAktivitetskort();
        assertThat(aktivitetskort.getAktivitetStatus()).isEqualTo(AktivitetskortStatus.PLANLAGT);
        assertThat(aktivitetskort.getTittel()).isEqualTo("The Elder Scrolls");
        assertThat(aktivitetskort.getBeskrivelse()).isEqualTo("aktivitetsbeskrivelse");
    }
}
