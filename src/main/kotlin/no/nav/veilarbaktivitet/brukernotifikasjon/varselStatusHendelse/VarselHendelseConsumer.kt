package no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse

import no.nav.veilarbaktivitet.brukernotifikasjon.BrukerNotifikasjonDAO
import no.nav.veilarbaktivitet.brukernotifikasjon.kvittering.KvitteringDAO
import no.nav.veilarbaktivitet.brukernotifikasjon.kvittering.KvitteringMetrikk
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.MinSideBrukernotifikasjonsId
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
open class VarselHendelseConsumer(
    val kvitteringDAO: KvitteringDAO,
    val brukerNotifikasjonDAO: BrukerNotifikasjonDAO,
    val kvitteringMetrikk: KvitteringMetrikk,
    @Value("\${app.env.appname}")
    val appname: String,
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
            is EksternVarselHendelseDTO -> behandleEskternVarselHendelse(melding)
            is InternVarselHendelseDTO -> behandleInternVarselHendelse(melding)
        }
    }

    open fun behandleInternVarselHendelse(hendelse: InternVarselHendelseDTO) {

    }

    open fun behandleEskternVarselHendelse(hendelse: EksternVarselHendelseDTO) {
        val varselId = hendelse.varselId.toString()
        log.info(
            "Konsumerer Ekstern-varsel-hendelse varselId={}, status={}",
            varselId,
            hendelse.status
        )

        if (!brukerNotifikasjonDAO.finnesBrukernotifikasjon(MinSideBrukernotifikasjonsId(UUID.fromString(varselId)))) {
            log.warn(
                "Mottok kvittering for brukernotifikasjon bestillingsid={} som ikke finnes i våre systemer",
                varselId
            )
            throw IllegalArgumentException("Ugyldig bestillingsid.")
        }

        kvitteringDAO.lagreKvitering(varselId, Kvittering(
            status = hendelse.status.name,
            melding = hendelse.feilmelding,
        ), hendelse)


        when (hendelse.status) {
            EksternVarselStatus.bestilt -> {}
            EksternVarselStatus.sendt -> {
                // Kan komme første gang og på resendinger
                kvitteringDAO.setFullfortForGyldige(varselId)
                log.info(
                    "Brukernotifikasjon fullført for bestillingsId={}",
                    varselId
                )
                /**
                 * Håndterer race condition der brukernotifikasjon blir satt til done fra andre systemer
                 * (typisk ved at bruker er inne og leser på ditt nav)
                 * før eksternt varsel er sendt ut
                 */
                kvitteringDAO.setAvsluttetHvisVarselKvitteringStatusErIkkeSatt(varselId)
            }
            EksternVarselStatus.feilet -> {
                log.warn(
                    "varsel feilet for notifikasjon bestillingsId={} med melding {}",
                    varselId,
                    hendelse.feilmelding
                )
                kvitteringDAO.setFeilet(varselId)
            }
            // Disse statusene finnes ikke lenger
//            EksternVarslingKvitteringConsumer.INFO, EksternVarslingKvitteringConsumer.OVERSENDT -> {}
//            EksternVarslingKvitteringConsumer.FERDIGSTILT -> if (melding.getDistribusjonId() != null) {}
        }

//        if (melding.getDistribusjonId() == null) {
            kvitteringMetrikk.incrementBrukernotifikasjonKvitteringMottatt(hendelse.status.name)
//        }
    }

}