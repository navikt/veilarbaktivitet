package no.nav.veilarbaktivitet.service;

import static org.mockito.ArgumentMatchers.eq;


//TOODO fiks denne
/*
public class KafkaConsumerOnpremServiceTest {

    @Test
    public void skal_behandleOppfolgingAvsluttet() {
        AktivitetService aktivitetService = mock(AktivitetService.class);
        KafkaConsumerOnpremService consumerService = new KafkaConsumerOnpremService(aktivitetService, null);

        ZonedDateTime sluttdato = ZonedDateTime.of(2020, 4, 5, 16, 17, 0, 0, ZoneId.systemDefault());
        Date expectedSluttdato = new Date(sluttdato.toInstant().toEpochMilli());

        OppfolgingAvsluttetKafkaDTO oppfolgingAvsluttetDto = new OppfolgingAvsluttetKafkaDTO();
        oppfolgingAvsluttetDto.setAktorId("1234");
        oppfolgingAvsluttetDto.setSluttdato(sluttdato);

        consumerService.behandleOppfolgingAvsluttet(oppfolgingAvsluttetDto);

        verify(aktivitetService, times(1)).settAktiviteterTilHistoriske(Person.aktorId("1234"), expectedSluttdato);
    }

    @Test
    public void skal_behandleKvpAvsluttet() {
        AktivitetService aktivitetService = mock(AktivitetService.class);
        KVPAvsluttetService kvpAvsluttetService = mock(KVPAvsluttetService.class);
        KafkaConsumerOnpremService consumerService = new KafkaConsumerOnpremService(aktivitetService, kvpAvsluttetService);

        ZonedDateTime sluttdato = ZonedDateTime.of(2020, 4, 5, 16, 17, 0, 0, ZoneId.systemDefault());
        Date expectedSluttdato = new Date(sluttdato.toInstant().toEpochMilli());

        KvpAvsluttetKafkaDTO kvpAvsluttetDto = new KvpAvsluttetKafkaDTO();
        kvpAvsluttetDto.setAktorId("1234");
        kvpAvsluttetDto.setAvsluttetBegrunnelse("begrunnelse");
        kvpAvsluttetDto.setAvsluttetDato(sluttdato);

        consumerService.behandleKvpAvsluttet(kvpAvsluttetDto);

        verify(kvpAvsluttetService, times(1)).settAktiviteterInomKVPPeriodeTilAvbrutt(
                Person.aktorId("1234"), "begrunnelse", expectedSluttdato
        );
    }

}
*/