package no.nav.veilarbaktivitet.oversikten

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.json.JsonUtils
import no.nav.common.utils.EnvironmentUtils
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.aktivitet.AktivitetId
import no.nav.veilarbaktivitet.oversikten.OversiktenMelding.Kategori.UDELT_SAMTALEREFERAT
import no.nav.veilarbaktivitet.person.Person.AktorId
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

@Service
open class OversiktenService(
    private val aktorOppslagClient: AktorOppslagClient,
    private val oversiktenMeldingMedMetadataRepository: OversiktenMeldingMedMetadataDAO,
    private val oversiktenMeldingAktivitetMappingDao: OversiktenMeldingAktivitetMappingDAO,
    private val oversiktenProducer: OversiktenProducer,
    private val aktivitetDAO: AktivitetDAO

) {
    private val log = LoggerFactory.getLogger(OversiktenService::class.java)
    private val erProd = EnvironmentUtils.isProduction().orElse(false)

    @Scheduled(cron = "0 * * * * *") // Hvert minutt
    @SchedulerLock(name = "oversikten_melding_med_metadata_scheduledTask", lockAtMostFor = "PT2M")
    open fun sendUsendteMeldingerTilOversikten() {
        val meldingerMedMetadata = oversiktenMeldingMedMetadataRepository.hentAlleSomSkalSendes().sortedBy { it.opprettet }
        if (meldingerMedMetadata.isNotEmpty()) {
            log.info("Sender ${meldingerMedMetadata.size} meldinger til oversikten")
        }
        meldingerMedMetadata.forEach { meldingMedMetadata ->
            oversiktenProducer.sendMelding(
                meldingMedMetadata.meldingKey.toString(),
                meldingMedMetadata.meldingSomJson
            )
            oversiktenMeldingMedMetadataRepository.markerSomSendt(meldingMedMetadata.id)
            meldingMedMetadata.fnr
        }
    }

    /**
     * Engangsjobb for å rydde opp i tilfeller der det er sendt START-melding til oversikten,
     * men referatet allerede er publisert (referat_publisert = 1).
     */
    @Scheduled(cron = "0 50 12 * * ?")
    @SchedulerLock(name = "oversikten_stopp_publiserte_referat_scheduledTask", lockAtMostFor = "PT15M")
    open fun sendStoppMeldingForPubliserteReferaterMedFeilaktigStartMelding() {
        log.info("Starter opprydding av feilaktige START-meldinger for publiserte referater")
        val feilaktigeStartMeldinger = oversiktenMeldingMedMetadataRepository.hentPubliserteSamtalereferaterMedStartMeldingUtenStoppMelding()
        log.info("Fant ${feilaktigeStartMeldinger.size} publiserte referater med START-melding uten STOPP-melding")
        feilaktigeStartMeldinger.forEach {
            log.info("Sender STOPP-melding for aktivitet ${it.aktivitetId}")
            lagreStoppMeldingOmUdeltSamtalereferatIUtboks(AktorId(it.aktorId.get()), it.aktivitetId)
        }
        log.info("Ferdig med opprydding av feilaktige START-meldinger")
    }


    open fun lagreStartMeldingOmUdeltSamtalereferatIUtboks(aktorId: AktorId, aktivitetId: AktivitetId) {
        val eksisterendeMeldingKey = oversiktenMeldingAktivitetMappingDao.hentMeldingKeyForAktivitet(aktivitetId, UDELT_SAMTALEREFERAT)
        if (eksisterendeMeldingKey != null) {
            log.info("Sender STOPP på eksisterende melding-key $eksisterendeMeldingKey før ny START for aktivitet $aktivitetId")
            lagreStoppMeldingOmUdeltSamtalereferatIUtboks(aktorId, aktivitetId)
        }

        val fnr = aktorOppslagClient.hentFnr(no.nav.common.types.identer.AktorId.of(aktorId.get()))
        val melding =
            OversiktenMelding.forUdeltSamtalereferat(fnr.toString(), OversiktenMelding.Operasjon.START, erProd)
        val oversiktenMeldingMedMetadata = OversiktenMeldingMedMetadata(
            meldingSomJson = JsonUtils.toJson(melding),
            fnr = fnr,
            kategori = melding.kategori,
            meldingKey = UUID.randomUUID(),
            operasjon = melding.operasjon,
        )
        oversiktenMeldingMedMetadataRepository.lagre(oversiktenMeldingMedMetadata)
        oversiktenMeldingAktivitetMappingDao.lagreKoblingMellomOversiktenMeldingOgAktivitet(
            aktivitetId = aktivitetId,
            oversiktenMeldingKey = oversiktenMeldingMedMetadata.meldingKey,
            kategori = UDELT_SAMTALEREFERAT
        )
    }

    open fun lagreStoppMeldingOmUdeltSamtalereferatIUtboks(aktorId: AktorId, aktivitetId: AktivitetId) {
        val meldingKey = oversiktenMeldingAktivitetMappingDao.hentMeldingKeyForAktivitet(aktivitetId, UDELT_SAMTALEREFERAT)
        if (meldingKey == null) {
            log.info("Ikke behov for stopp-melding for aktivitet med id $aktivitetId og kategori $UDELT_SAMTALEREFERAT")
            return
        }

        val fnr = aktorOppslagClient.hentFnr(no.nav.common.types.identer.AktorId.of(aktorId.get()))
        val sluttmelding =
            OversiktenMelding.forUdeltSamtalereferat(fnr.toString(), OversiktenMelding.Operasjon.STOPP, erProd)

        val oversiktenMeldingMedMetadata = OversiktenMeldingMedMetadata(
            meldingSomJson = JsonUtils.toJson(sluttmelding),
            fnr = fnr,
            kategori = sluttmelding.kategori,
            meldingKey = meldingKey,
            operasjon = sluttmelding.operasjon,
        )
        oversiktenMeldingMedMetadataRepository.lagre(oversiktenMeldingMedMetadata)
    }

    fun lagreStoppMeldingVedAvsluttOppfolging(aktorId: AktorId) {
        val fnr = aktorOppslagClient.hentFnr(no.nav.common.types.identer.AktorId.of(aktorId.get()))

        val meldingerSomSkalAvsluttes =
            oversiktenMeldingAktivitetMappingDao.hentAktivitetsIdForMeldingerSomSkalAvsluttes(fnr)

        for (aktivitetId in meldingerSomSkalAvsluttes) {
            lagreStoppMeldingOmUdeltSamtalereferatIUtboks(aktorId, aktivitetId)
        }
    }

    fun lagreOppdaterSisteMøteMelding(aktorId: AktorId) {
        val møter = aktivitetDAO.hentAlleMoterForPerson(aktorId)
        oversiktenProducer.sendSisteAktivitetHendelse(
            aktorId.get(),
            møter.finnTidspunkter()?.let { JsonUtils.toJson(it) }
        )
    }
}

fun List<AktivitetsplanMote>.finnTidspunkter(): SisteAktivitetDto? {
    if (this.isEmpty()) return null
    val now = Instant.now()
    val sorted = this.sortedBy { it.tidspunkt }
    return SisteAktivitetDto(
        sorted.filter { it.tidspunkt.isBefore(now) }
            .maxByOrNull { it.tidspunkt }
            ?.let { Mote( it.tidspunkt, it.id) },
        sorted.firstOrNull { it.tidspunkt.isAfter(now) }?.let { Mote( it.tidspunkt, it.id) }
    )
}

data class Mote(
    val tidspunkt: Instant,
    val id: Long,
)

/* Aktivtet som i noe har skjedd, ikke som i aktivitetsplanen-aktivitet */
class SisteAktivitetDto(
    val sisteGjennomforteMote: Mote?,
    val nestePlanlagteMote: Mote?,
)

data class AktivitetsplanMote(
    val id: Long,
    /**
     *  Fra-tidspunkt på møter eller samtalereferat. Samtalererat settes bruker fra-dato starten av dagen
     *  */
    val tidspunkt: Instant,
)