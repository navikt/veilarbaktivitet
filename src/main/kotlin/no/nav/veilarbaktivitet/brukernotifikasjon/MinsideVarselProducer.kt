package no.nav.veilarbaktivitet.brukernotifikasjon

import no.nav.tms.varsel.action.*
import no.nav.tms.varsel.builder.VarselActionBuilder
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SkalSendes
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringTemplate
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class MinsideVarselProducer(
    @Value("\${topic.ut.brukernotifikasjon.brukervarsel}")
    private var brukervarselTopic: String,
    @Value("\${app.env.appname}")
    private var appname: String,
    @Value("\${app.env.namespace}")
    private val namespace: String,
    @Value("\${app.env.cluster}")
    private val cluster: String,
    private val producer: KafkaStringTemplate
) {

    private val log = LoggerFactory.getLogger(MinsideVarselProducer::class.java)

    private fun toBrukerVarsel(skalSendes: SkalSendes, varselType: Varseltype): String {
        return VarselActionBuilder.opprett {
            produsent = Produsent(
                cluster = cluster,
                namespace = namespace,
                appnavn = appname
            )
            type = varselType
            sensitivitet = Sensitivitet.Substantial
            varselId = skalSendes.varselId.toString()
            aktivFremTil = ZonedDateTime.now().plusMonths(1)
            ident = skalSendes.fnr.get()
            tekst = Tekst(
                spraakkode = "nb",
                tekst = skalSendes.melding,
                default = true
            )
            link = skalSendes.url.toString()
            eksternVarsling = EksternVarslingBestilling(
                prefererteKanaler = listOf(EksternKanal.SMS),
                smsVarslingstekst = skalSendes.smsTekst,
                epostVarslingstittel = skalSendes.epostTitel,
                epostVarslingstekst = skalSendes.epostBody
            )
        }
    }

    private fun publiserPåBrukernotifikasjonTopic(skalSendes: SkalSendes, varselType: Varseltype): Long {
        val opprettVarsel = toBrukerVarsel(skalSendes, varselType)
        val record = ProducerRecord(brukervarselTopic, skalSendes.varselId.toString(), opprettVarsel)
        return producer.send(record).get().recordMetadata.offset()
    }

    open fun send(skalSendes: SkalSendes): Long {
        val varselType = when (skalSendes.varselType.brukernotifikasjonType) {
            BrukernotifikasjonsType.OPPGAVE -> Varseltype.Oppgave
            BrukernotifikasjonsType.BESKJED -> Varseltype.Beskjed
        }
        log.info("Oppretter varsel med varselId {} og varselType {}", skalSendes.varselId, varselType)
        return publiserPåBrukernotifikasjonTopic(skalSendes, varselType)
    }

}