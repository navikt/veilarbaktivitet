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

    @Scheduled(cron = "0 */1 * * * *") // Hvert minutt
    @SchedulerLock(name = "oversikten_melding_med_metadata_scheduledTask", lockAtMostFor = "PT3M")
    open fun sendUsendteMeldingerTilOversikten() {
        val meldingerMedMetadata = oversiktenMeldingMedMetadataRepository.hentAlleSomSkalSendes()
        if(meldingerMedMetadata.isNotEmpty()) {
            log.info("Sender ${meldingerMedMetadata.size} meldinger til oversikten")
        }
        meldingerMedMetadata.forEach { meldingMedMetadata ->
            oversiktenProducer.sendMelding(meldingMedMetadata.meldingKey.toString(), meldingMedMetadata.meldingSomJson)
            oversiktenMeldingMedMetadataRepository.markerSomSendt(meldingMedMetadata.id)
            meldingMedMetadata.fnr
        }
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
        val fnr = aktorOppslagClient.hentFnr(no.nav.common.types.identer.AktorId.of(aktorId.get()))
        val sluttmelding = OversiktenMelding.forUdeltSamtalereferat(fnr.toString(), OversiktenMelding.Operasjon.STOPP, erProd)
        val meldingKey = oversiktenMeldingAktivitetMappingDao.hentMeldingKeyForAktivitet(aktivitetId, UDELT_SAMTALEREFERAT)

        if (meldingKey == null) {
            log.warn("Finner ikke meldingKey for aktivitet med id $aktivitetId og kategori $UDELT_SAMTALEREFERAT")
            return
        }

        val oversiktenMeldingMedMetadata = OversiktenMeldingMedMetadata(
            meldingSomJson = JsonUtils.toJson(sluttmelding),
            fnr = fnr,
            kategori = sluttmelding.kategori,
            meldingKey = meldingKey,
            operasjon = sluttmelding.operasjon,
        )
        oversiktenMeldingMedMetadataRepository.lagre(oversiktenMeldingMedMetadata)
    }
}