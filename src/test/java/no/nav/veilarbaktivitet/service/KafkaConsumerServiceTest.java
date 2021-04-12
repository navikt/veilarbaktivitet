package no.nav.veilarbaktivitet.service;

import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.domain.kafka.OppfolgingAvsluttetKafkaDTO;
import no.nav.veilarbaktivitet.kvp.KvpAvsluttetKafkaDTO;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class KafkaConsumerServiceTest {

    @Test
    public void skal_behandleOppfolgingAvsluttet() {
        AktivitetService aktivitetService = mock(AktivitetService.class);
        KafkaConsumerService consumerService = new KafkaConsumerService(aktivitetService);

        ZonedDateTime sluttdato = ZonedDateTime.of(2020, 4, 5, 16, 17, 0, 0, ZoneId.systemDefault());
        Date expectedSluttdato = new Date(sluttdato.toInstant().toEpochMilli());

        OppfolgingAvsluttetKafkaDTO oppfolgingAvsluttetDto = new OppfolgingAvsluttetKafkaDTO();
        oppfolgingAvsluttetDto.setAktorId("1234");
        oppfolgingAvsluttetDto.setSluttdato(sluttdato);

        consumerService.behandleOppfolgingAvsluttet(oppfolgingAvsluttetDto);

        verify(aktivitetService, times(1)).settAktiviteterTilHistoriske(eq(Person.aktorId("1234")), eq(expectedSluttdato));
    }

    @Test
    public void skal_behandleKvpAvsluttet() {
        AktivitetService aktivitetService = mock(AktivitetService.class);
        KafkaConsumerService consumerService = new KafkaConsumerService(aktivitetService);

        ZonedDateTime sluttdato = ZonedDateTime.of(2020, 4, 5, 16, 17, 0, 0, ZoneId.systemDefault());
        Date expectedSluttdato = new Date(sluttdato.toInstant().toEpochMilli());

        KvpAvsluttetKafkaDTO kvpAvsluttetDto = new KvpAvsluttetKafkaDTO();
        kvpAvsluttetDto.setAktorId("1234");
        kvpAvsluttetDto.setAvsluttetBegrunnelse("begrunnelse");
        kvpAvsluttetDto.setAvsluttetDato(sluttdato);

        consumerService.behandleKvpAvsluttet(kvpAvsluttetDto);

        verify(aktivitetService, times(1)).settAktiviteterInomKVPPeriodeTilAvbrutt(
                eq(Person.aktorId("1234")), eq("begrunnelse"), eq(expectedSluttdato)
        );
    }

}
