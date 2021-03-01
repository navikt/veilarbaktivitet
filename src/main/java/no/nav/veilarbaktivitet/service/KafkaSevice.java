package no.nav.veilarbaktivitet.service;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.kafka.dto.KvpAvsluttetKafkaDTO;
import no.nav.veilarbaktivitet.kafka.dto.OppfolgingAvsluttetKafkaDTO;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class KafkaSevice {

    private final AktivitetService aktivitetService;

    public void konsumerOppfolgingAvsluttet(OppfolgingAvsluttetKafkaDTO oppfolgingAvsluttetKafkaDTO) {
        Person.AktorId aktorId = Person.aktorId(oppfolgingAvsluttetKafkaDTO.getAktorId());
        Date sluttDato = new Date(oppfolgingAvsluttetKafkaDTO.getSluttdato().toInstant().toEpochMilli());

        aktivitetService.settAktiviteterTilHistoriske(aktorId, sluttDato);
    }

    public void konsumerKvpAvsluttet(KvpAvsluttetKafkaDTO kvpAvsluttetKafkaDTO) {
        Person.AktorId aktorId = Person.aktorId(kvpAvsluttetKafkaDTO.getAktorId());
        String begrunnelse = kvpAvsluttetKafkaDTO.getAvsluttetBegrunnelse();
        Date sluttDato = new Date(kvpAvsluttetKafkaDTO.getAvsluttetDato().toInstant().toEpochMilli());

        aktivitetService.settAktiviteterInomKVPPeriodeTilAvbrutt(aktorId, begrunnelse, sluttDato);
    }

}
