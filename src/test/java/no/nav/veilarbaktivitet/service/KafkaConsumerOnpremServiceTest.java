package no.nav.veilarbaktivitet.service;


//TOODO fiks denne
/*
public class KafkaConsumerOnpremServiceTest {

    @Test
    void skal_behandleKvpAvsluttet() {
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
