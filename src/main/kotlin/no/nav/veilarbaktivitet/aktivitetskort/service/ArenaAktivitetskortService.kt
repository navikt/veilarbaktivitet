package no.nav.veilarbaktivitet.aktivitetskort.service

import lombok.extern.slf4j.Slf4j
import no.nav.veilarbaktivitet.aktivitet.AktivitetService
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetIdMappingProducer
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortMapper.toAktivitetsDataInsert
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMapping
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDAO
import no.nav.veilarbaktivitet.aktivitetskort.service.ArenaMigreringsStatus.*
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDAO
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.VarselDAO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Slf4j
@Service
class ArenaAktivitetskortService (
    private val forhaandsorienteringDAO: ForhaandsorienteringDAO,
    private val varselDAO: VarselDAO,
    private val idMappingDAO: IdMappingDAO,
    private val aktivitetService: AktivitetService,
    private val aktivitetIdMappingProducer: AktivitetIdMappingProducer,
    private val tiltakMigreringDAO: TiltakMigreringDAO
) {
    private val log = LoggerFactory.getLogger(javaClass)
    fun opprettAktivitet(bestilling: ArenaAktivitetskortBestilling): AktivitetData? {
        // Opprett via AktivitetService
        val aktivitetsData = bestilling.toAktivitetsDataInsert()
        val opprettetAktivitetsData = aktivitetService.opprettAktivitet(aktivitetsData)
        val idMapping = bestilling.idMapping(opprettetAktivitetsData.id)

        // Gjør arena-spesifikk migrering hvis ikke migrert allerede
        when (bestilling.finnMigreringsStatus()) {
            ER_MIGRERT -> {}
            IKKE_MIGRERT -> arenaspesifikkMigrering(opprettetAktivitetsData, idMapping)
            // Aktivitet er migrert men legger til splitt-kort i id-mapping
            ER_MIGRERT_NY_PERIODE_SPLITT -> idMappingDAO.insert(idMapping)
        }
        return opprettetAktivitetsData
    }

    /** Gjør følgende:
     * 1. Legger til i id-mapping
     * 2. Flytter FHO fra arenaId hvis FHO finnes
     * 3. Flytter brukernotifikasjoner hvis de finnes
     * 4. Send id-mapping til dialog
     * **/
    private fun arenaspesifikkMigrering(
        opprettetAktivitet: AktivitetData,
        idMapping: IdMapping,
    ) {
        idMappingDAO.insert(idMapping)
        forhaandsorienteringDAO.getFhoForArenaAktivitet(idMapping.arenaId)
            ?.let { fho -> tiltakMigreringDAO.flyttFHOTilAktivitet(fho.id, opprettetAktivitet.id) }

        // oppdater alle brukernotifikasjoner med aktivitet arena-ider
        varselDAO.updateAktivitetIdForArenaBrukernotifikasjon(
            opprettetAktivitet.id,
            opprettetAktivitet.versjon,
            idMapping.arenaId
        )
        // Send idmapping til dialog
        aktivitetIdMappingProducer.publishAktivitetskortIdMapping(idMapping)
    }

    fun dobbelsjekkMigrering(
        bestilling: ArenaAktivitetskortBestilling,
        opprettetAktivitet: AktivitetData): Boolean {
        val idMapping = bestilling.idMapping(opprettetAktivitet.id)
        return when (bestilling.finnMigreringsStatus()) {
            ER_MIGRERT -> false
            IKKE_MIGRERT -> {
                arenaspesifikkMigrering(opprettetAktivitet, idMapping)
                true
            }
            ER_MIGRERT_NY_PERIODE_SPLITT -> {
                // Only insert mapping
                idMappingDAO.insert(idMapping)
                true
            }
        }.also { bleMigrert ->
            val id = bestilling.aktivitetskort.id
            if (bleMigrert) {
                log.info("Aktivitet tatt over av annet team men var ikke migrert, gjorde arena-migrering og ignorerte data fra acl $id")
            } else {
                log.info("Aktivitet tatt over av annet team. Ignorerer melding fra aktivitet arena acl $id")
            }
        }
    }

    private fun ArenaAktivitetskortBestilling.finnMigreringsStatus(): ArenaMigreringsStatus {
        val ideer = idMappingDAO.getAktivitetIder(this.eksternReferanseId).map { it.funksjonellId }
        return when {
            ideer.contains(this.aktivitetskort.id) -> ER_MIGRERT
            ideer.isEmpty() -> IKKE_MIGRERT
            else -> ER_MIGRERT_NY_PERIODE_SPLITT
        }

    }
}

fun ArenaAktivitetskortBestilling.idMapping(aktivitetId: Long) = IdMapping(
    this.eksternReferanseId,
    aktivitetId,
    this.aktivitetskort.id
)
enum class ArenaMigreringsStatus {
    ER_MIGRERT,
    ER_MIGRERT_NY_PERIODE_SPLITT,
    IKKE_MIGRERT
}