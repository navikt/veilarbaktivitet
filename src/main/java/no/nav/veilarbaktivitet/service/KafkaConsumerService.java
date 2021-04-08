package no.nav.veilarbaktivitet.service;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.domain.kafka.OppfolgingAvsluttetKafkaDTO;
import no.nav.veilarbaktivitet.kvp.KvpAvsluttetKafkaDTO;
import org.springframework.stereotype.Service;

import java.util.Date;

@RequiredArgsConstructor
@Service
public class KafkaConsumerService {

    private final AktivitetService aktivitetService;

    public void behandleOppfolgingAvsluttet(OppfolgingAvsluttetKafkaDTO oppfolgingAvsluttetDto) {
        Person.AktorId aktorId = Person.aktorId(oppfolgingAvsluttetDto.getAktorId());
        Date sluttDato = new Date(oppfolgingAvsluttetDto.getSluttdato().toInstant().toEpochMilli());

        aktivitetService.settAktiviteterTilHistoriske(aktorId, sluttDato);
    }

    public void behandleKvpAvsluttet(KvpAvsluttetKafkaDTO kvpAvsluttetDto) {
        Person.AktorId aktorId = Person.aktorId(kvpAvsluttetDto.getAktorId());
        String begrunnelse = kvpAvsluttetDto.getAvsluttetBegrunnelse();
        Date sluttDato = new Date(kvpAvsluttetDto.getAvsluttetDato().toInstant().toEpochMilli());

        aktivitetService.settAktiviteterInomKVPPeriodeTilAvbrutt(aktorId, begrunnelse, sluttDato);
    }

}
