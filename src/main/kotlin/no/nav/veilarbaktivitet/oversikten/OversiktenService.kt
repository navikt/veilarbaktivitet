package no.nav.veilarbaktivitet.oversikten

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.json.JsonUtils
import no.nav.common.utils.EnvironmentUtils
import no.nav.veilarbaktivitet.aktivitet.AktivitetId
import no.nav.veilarbaktivitet.oversikten.OversiktenMelding.Kategori.UDELT_SAMTALEREFERAT
import no.nav.veilarbaktivitet.person.Person.AktorId
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.*

@Service
open class OversiktenService(
    private val aktorOppslagClient: AktorOppslagClient,
    private val oversiktenMeldingMedMetadataRepository: OversiktenMeldingMedMetadataDAO,
    private val oversiktenMeldingAktivitetMappingDao: OversiktenMeldingAktivitetMappingDAO,
    private val oversiktenProducer: OversiktenProducer

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

    //    Brukt til å hente gamle udelte samtalereferater der det ikke allerede er sendt melding.
    //    Dette er en engangsjobb som ble kjørt i PROD 20.12.24 kl 13:50
    //    Koden skal stå inntil videre, i tilfelle det dukker opp noen bugs
    //    Kjørt igjen 26.11.25 kl 13
    //    @Scheduled(cron = "0 0 13 * * ?")
    //    @SchedulerLock(name = "oversikten_melding_gamle_udelte_scheduledTask", lockAtMostFor = "PT15M")
    open fun sendAlleGamleUdelte() {
        log.info("Starter henting av udelte samtalereferater i åpen periode")
        val alleUdelte =
            oversiktenMeldingMedMetadataRepository.hentAlleUdelteSamtalereferaterIÅpenPeriode()
        log.info("antall udelte referat i åpen periode: ${alleUdelte.size}")
        alleUdelte.forEach {
            lagreStartMeldingOmUdeltSamtalereferatIUtboks(AktorId(it.aktorId.get()), it.aktivitetId)
        }
    }

//        @Scheduled(cron = "0 50 14 * * ?")
//        @SchedulerLock(name = "oversikten_melding_gamle_udelte_scheduledTask", lockAtMostFor = "PT15M")
    open fun sendStoppMeldingPåAlleUdelteSamtalereferatIAvbruttAktivitet() {
        log.info("Starter henting av udelte samtalereferater i avbrutt aktivitet")
        val alleUdelte = oversiktenMeldingMedMetadataRepository.hentAlleUdelteSamtalereferaterIAvbruttAktivitet()
        log.info("antall udelte referat i avbrutt aktivitet: ${alleUdelte.size}")
        alleUdelte.forEach {
            lagreStoppMeldingOmUdeltSamtalereferatIUtboks(AktorId(it.aktorId.get()), it.aktivitetId)
        }
    }

    /**
     * Engangsjobb for å rydde opp i tilfeller der det er sendt START-melding til oversikten,
     * men referatet allerede er publisert (referat_publisert = 1).
     */
//    @Scheduled(cron = "0 6 16 * * ?")
//    @SchedulerLock(name = "oversikten_stopp_publiserte_referat_scheduledTask", lockAtMostFor = "PT15M")
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
}