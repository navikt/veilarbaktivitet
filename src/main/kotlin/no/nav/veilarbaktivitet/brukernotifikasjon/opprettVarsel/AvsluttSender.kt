package no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel

import io.micrometer.core.annotation.Timed
import lombok.RequiredArgsConstructor
import lombok.SneakyThrows
import lombok.extern.slf4j.Slf4j
import no.nav.tms.varsel.action.Produsent
import no.nav.tms.varsel.builder.VarselActionBuilder
import no.nav.veilarbaktivitet.brukernotifikasjon.avslutt.AvsluttDao
import no.nav.veilarbaktivitet.brukernotifikasjon.avslutt.SkalAvluttes
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringTemplate
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@RequiredArgsConstructor
@Slf4j
internal open class AvsluttSender(
    private val producer: KafkaStringTemplate,
    private val avsluttDao: AvsluttDao,
    @Value("\${topic.ut.brukernotifikasjon.brukervarsel}")
    private val brukervarselTopic: String,
    @Value("\${app.env.appname}")
    private val appname: String,
    @Value("\${app.env.namespace}")
    private val namespace: String,
    @Value("\${app.env.cluster}")
    private val cluster: String
) {

    private val log = LoggerFactory.getLogger(AvsluttSender::class.java)

    @SneakyThrows
    @Transactional
    @Timed(value = "brukernotifikasjon_avslutt_oppgave_sendt")
    open fun avsluttOppgave(skalAvluttes: SkalAvluttes) {
        val brukernotifikasjonId = skalAvluttes.brukernotifikasjonId

        val markertAvsluttet = avsluttDao.markerOppgaveSomAvsluttet(brukernotifikasjonId)
        if (markertAvsluttet) {
            val melding = VarselActionBuilder.inaktiver {
                this.varselId = brukernotifikasjonId
                this.produsent = Produsent(
                    cluster = cluster,
                    namespace = namespace,
                    appnavn = appname,
                )
            }
            val kafkaMelding = ProducerRecord(brukervarselTopic, skalAvluttes.brukernotifikasjonId, melding)
            log.info(
                "Sender brukernotifikasjon 'done' for grupperingsid: {}",
                skalAvluttes.getOppfolgingsperiode().toString()
            )
            producer.send(kafkaMelding).get()
        }
    }

    open fun getOppgaverSomSkalAvbrytes(maxAntall: Int): List<SkalAvluttes> {
        return avsluttDao.getOppgaverSomSkalAvsluttes(maxAntall)
    }

    open fun avsluttIkkeSendteOppgaver(): Int {
        return avsluttDao.avsluttIkkeSendteOppgaver()
    }

    open fun markerAvslutteterAktiviteterSomSkalAvsluttes(): Int {
        return avsluttDao.markerAvslutteterAktiviteterSomSkalAvsluttes()
    }
}
