package no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse

import no.nav.veilarbaktivitet.brukernotifikasjon.kvittering.KvitteringDAO
import no.nav.veilarbaktivitet.brukernotifikasjon.kvittering.VarselHendelseMetrikk
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.MinSideBrukernotifikasjonsId
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.VarselDAO
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class VarselHendelseConsumer(
    val varselDAO: VarselDAO,
    val kvitteringDAO: KvitteringDAO,
    val varselHendelseMetrikk: VarselHendelseMetrikk,
) {

    val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    @KafkaListener(
        topics = ["\${topic.inn.brukernotifikasjon.brukervarselHendelse}"],
        containerFactory = "stringStringKafkaListenerContainerFactory",
        autoStartup = "\${app.kafka.enabled:false}"
    )
    open fun consume(kafkaRecord: ConsumerRecord<String?, String>) {
        val melding = kafkaRecord.value().deserialiserVarselHendelse()
        when (melding) {
            is EksternVarsling -> behandleEksternVarselHendelse(melding)
            is InternVarselHendelseDTO -> behandleInternVarselHendelse(melding)
            is VarselFraAnnenApp -> {}
        }
    }

    open fun behandleInternVarselHendelse(hendelse: InternVarselHendelseDTO) {

    }

    open fun behandleEksternVarselHendelse(hendelse: EksternVarsling) {
        val varselId = MinSideBrukernotifikasjonsId(hendelse.varselId)
        log.info(
            "Konsumerer Ekstern-varsel-hendelse varselId={}, varselType={}",
            varselId,
            hendelse.javaClass.simpleName
        )

        if (!varselDAO.finnesBrukernotifikasjon(varselId)) {
            log.warn(
                "Mottok kvittering for brukernotifikasjon varselId={} som ikke finnes i våre systemer",
                varselId
            )
            throw IllegalArgumentException("Ugyldig varselId.")
        }

//        kvitteringDAO.lagreKvitering(varselId, Kvittering(
//            status = hendelse.status.name,
//            melding = hendelse.feilmelding,
//        ), hendelse)

        when (hendelse) {
            is Sendt -> {
                kvitteringDAO.setEksternVarselStatusOK(varselId)
                log.info("Ekstern varsel sendt for varselId={}", varselId)
            }
            is Renotifikasjon -> {}
            is Feilet -> {
                log.warn(
                    "varsel feilet for notifikasjon varselId={} med melding {}",
                    varselId,
                    hendelse.feilmelding
                )
                kvitteringDAO.setFeilet(varselId)
            }
            else -> {}
        }

        varselHendelseMetrikk.incrementBrukernotifikasjonKvitteringMottatt(hendelse)
    }

}