package no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse

import no.nav.veilarbaktivitet.brukernotifikasjon.kvittering.KvitteringDAO
import no.nav.veilarbaktivitet.brukernotifikasjon.kvittering.VarselHendelseMetrikk
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.VarselDAO
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

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
            is EksternVarselOppdatering -> behandleEksternVarselHendelse(melding)
            is InternVarselHendelse -> behandleInternVarselHendelse(melding)
            is VarselFraAnnenApp -> {}
        }
    }

    open fun behandleInternVarselHendelse(hendelse: InternVarselHendelse) {
        varselHendelseMetrikk.incrementInternVarselMetrikk(hendelse)
        log.info("Minside varsel (hendelse) av type {} er {} varselId {}", hendelse.varseltype.name, hendelse.eventName, hendelse.varselId)
        when (hendelse.eventName) {
            InternVarselHendelseType.opprettet -> {}
            InternVarselHendelseType.inaktivert -> {
                recordTidTilInaktivering(hendelse)
                varselDAO.setAvsluttetStatus(hendelse.varselId)
            }
            InternVarselHendelseType.slettet -> {}
        }
    }

    private fun recordTidTilInaktivering(hendelse: InternVarselHendelse) {
        val opprettet = varselDAO.hentOpprettetTidspunkt(hendelse.varselId)
        if (opprettet.isEmpty) return;
        val tidTilInaktivering = Duration.between(opprettet.get(), LocalDateTime.now())
        varselHendelseMetrikk.recordTidTilInaktivering(tidTilInaktivering)
    }

    open fun behandleEksternVarselHendelse(varsel: EksternVarselOppdatering) {
        val varselId = varsel.varselId

        if (!varselDAO.finnesBrukernotifikasjon(varselId)) {
            log.warn(
                "Mottok ekstern-varsel-hendelse for varsel varselId={} som ikke finnes i våre systemer",
                varselId
            )
            throw IllegalArgumentException("Ugyldig varselId.")
        }

        log.info("Minside varsel (ekstern) av type {} er {} varselId {}", varsel.varseltype, varsel.hendelseType, varselId);

        when (varsel) {
            is Bestilt -> {}
            is Venter -> {}
            is Sendt -> {
                kvitteringDAO.setBekreftetSendt(varselId)
            }
            is Renotifikasjon -> {}
            is Feilet -> {
                log.warn(
                    "Minside varsel (ekstern) feilet for varselId={} med melding {}",
                    varselId,
                    varsel.feilmelding
                )
                kvitteringDAO.setFeilet(varselId)
            }
            is Ferdigstilt, is Kansellert -> {
                kvitteringDAO.setFerdigStiltEllerKansellert(varselId)
            }
        }

        varselHendelseMetrikk.incrementVarselKvitteringMottatt(varsel)
    }

}