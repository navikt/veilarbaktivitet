package no.nav.veilarbaktivitet.brukernotifikasjon.varsel

import io.micrometer.core.annotation.Timed
import no.nav.tms.varsel.action.*
import no.nav.tms.varsel.builder.VarselActionBuilder
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonsType
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringTemplate
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
open class BrukernotifikasjonProducer(
    private val template: KafkaStringTemplate,
    private val dao: VarselDAO,
    @Value("\${topic.ut.brukernotifikasjon}")
    private val brukernotifikasjonTopic: String,
) {
    @Transactional
    @Timed(value = "brukernotifikasjon_opprett_oppgave_sendt")
    internal open fun send(skalSendes: SkalSendes) {
        val oppdatertOk = dao.setSendt(skalSendes.brukernotifikasjonLopeNummer)
        if(!oppdatertOk) {
            return
        }

        val varseltype = when (skalSendes.varselType.brukernotifikasjonType) {
            BrukernotifikasjonsType.OPPGAVE -> Varseltype.Oppgave
            BrukernotifikasjonsType.BESKJED -> Varseltype.Beskjed
        }

        val kafkaValueJson = VarselActionBuilder.opprett {
            type = varseltype
            varselId = skalSendes.brukernotifikasjonId
            sensitivitet = Sensitivitet.Substantial
            ident = skalSendes.fnr.get()
            tekst = Tekst(
                spraakkode = "nb",
                tekst = skalSendes.melding,
                default = true
            )
            aktivFremTil = ZonedDateTime.now(ZoneId.of("Z")).plusMonths(1)
            link = skalSendes.url.toString()
            eksternVarsling = EksternVarslingBestilling(
                prefererteKanaler = listOf(EksternKanal.SMS),
                smsVarslingstekst =  skalSendes.smsTekst,
                epostVarslingstittel = skalSendes.epostTitel,
                epostVarslingstekst = skalSendes.epostBody
            )
        }

        template.send(ProducerRecord(brukernotifikasjonTopic, skalSendes.brukernotifikasjonId, kafkaValueJson)).get()
    }
}