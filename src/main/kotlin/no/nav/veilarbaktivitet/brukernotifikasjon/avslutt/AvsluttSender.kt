package no.nav.veilarbaktivitet.brukernotifikasjon.avslutt

import io.micrometer.core.annotation.Timed
import lombok.extern.slf4j.Slf4j
import no.nav.tms.varsel.builder.VarselActionBuilder
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringTemplate
import no.nav.veilarbaktivitet.person.Person
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Slf4j
internal open class AvsluttSender(
    private val template: KafkaStringTemplate,
    private val avsluttDao: AvsluttDao,
    private val brukernotifikasjonTopic: String,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    @Timed(value = "brukernotifikasjon_avslutt_oppgave_sendt")
    open fun avsluttOppgave(skalAvluttes: SkalAvluttes) {
        val fnr: Person.Fnr = skalAvluttes.fnr
        val brukernotifikasjonId = skalAvluttes.brukernotifikasjonId

        val markertAvsluttet = avsluttDao.markerOppgaveSomAvsluttet(brukernotifikasjonId)
        if (markertAvsluttet) {
            val kafkaValueJson = VarselActionBuilder.inaktiver {
                varselId = brukernotifikasjonId
            }

            logger.info("Sender brukernotifikasjon 'done' for brukernotifikasjon: {}", brukernotifikasjonId)
            template.send(ProducerRecord(brukernotifikasjonTopic, brukernotifikasjonId, kafkaValueJson)).get()
        }
    }

    fun getOppgaverSomSkalAvbrytes(maxAntall: Int): List<SkalAvluttes> {
        return avsluttDao.getOppgaverSomSkalAvsluttes(maxAntall)
    }

    fun avsluttIkkeSendteOppgaver(): Int {
        return avsluttDao.avsluttIkkeSendteOppgaver()
    }

    fun markerAvslutteterAktiviteterSomSkalAvsluttes(): Int {
        return avsluttDao.markerAvslutteterAktiviteterSomSkalAvsluttes()
    }
}
