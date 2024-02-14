package no.nav.veilarbaktivitet.aktivitetskort.service

import lombok.extern.slf4j.Slf4j
import no.nav.veilarbaktivitet.aktivitet.AktivitetService
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetIdMappingProducer
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortMapper.toAktivitetsDataInsert
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortMapper.toAktivitetsDataUpdate
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMapping
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDAO
import no.nav.veilarbaktivitet.avtalt_med_nav.AvtaltMedNavService
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDAO
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukerNotifikasjonDAO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Slf4j
@Service
class ArenaAktivitetskortService (
    private val forhaandsorienteringDAO: ForhaandsorienteringDAO,
    private val brukerNotifikasjonDAO: BrukerNotifikasjonDAO,
    private val idMappingDAO: IdMappingDAO,
    private val aktivitetService: AktivitetService,
    private val aktivitetIdMappingProducer: AktivitetIdMappingProducer,
    private val avtaltMedNavService: AvtaltMedNavService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    fun opprettAktivitet(bestilling: ArenaAktivitetskortBestilling): AktivitetData? {
        // Opprett via AktivitetService
        val aktivitetsData = bestilling.toAktivitetsDataInsert()
        val opprettetAktivitetsData = aktivitetService.opprettAktivitet(aktivitetsData)
        val idMapping = bestilling.idMapping(opprettetAktivitetsData.id)

        // Gjør arena-spesifikk migrering hvis ikke migrert allerede
        when (bestilling.finnOpprettelseType()) {
            OpprettelseType.ER_MIGRERT -> {}
            OpprettelseType.IKKE_MIGRERT -> arenaspesifikkMigrering(opprettetAktivitetsData, idMapping)
            // Aktivitet er migret men legger til splitt-kort i id-mapping
            OpprettelseType.ER_MIGRERT_NY_PERIODE_SPLITT -> idMappingDAO.insert(idMapping)
        }
        return opprettetAktivitetsData
    }

    private fun arenaspesifikkMigrering(
        opprettetAktivitet: AktivitetData,
        idMapping: IdMapping,
    ) {
        idMappingDAO.insert(idMapping)
        forhaandsorienteringDAO.getFhoForArenaAktivitet(idMapping.arenaId)
            ?.let {fho ->
                val updated = forhaandsorienteringDAO.leggTilTekniskId(fho.id, opprettetAktivitet.getId())
                if (updated == 0) return@let
                aktivitetService.oppdaterAktivitet(
                    opprettetAktivitet,
                    opprettetAktivitet.withFhoId(fho.id)
                )
                log.debug(
                    "La til teknisk id på FHO med id={}, tekniskId={}",
                    fho.id,
                    opprettetAktivitet.getId()
                )
            }

        // oppdater alle brukernotifikasjoner med aktivitet arena-ider
        brukerNotifikasjonDAO.updateAktivitetIdForArenaBrukernotifikasjon(
            opprettetAktivitet.getId(),
            opprettetAktivitet.getVersjon(),
            idMapping.arenaId
        )
        // Send idmapping til dialog
        aktivitetIdMappingProducer.publishAktivitetskortIdMapping(idMapping)
    }

    fun dobbelsjekkMigrering(
        bestilling: ArenaAktivitetskortBestilling,
        opprettetAktivitet: AktivitetData): Boolean {
        val idMapping = bestilling.idMapping(opprettetAktivitet.id)
        val aktivitetIder = idMappingDAO.getAktivitetIder(bestilling.eksternReferanseId)
        return when {
            aktivitetIder.map { it.funksjonellId }.contains(bestilling.aktivitetskort.id) -> false
            aktivitetIder.isEmpty() -> {
                arenaspesifikkMigrering(opprettetAktivitet, idMapping)
                true
            }
            else -> {
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

    fun oppdaterAktivitet(
        bestilling: ArenaAktivitetskortBestilling,
        gammelAktivitet: AktivitetData
    ): AktivitetData {
        val oppfolgingsperiode = bestilling.oppfolgingsperiode
        val aktivitetsData = bestilling.toAktivitetsDataUpdate()
            .withId(gammelAktivitet.id)
            .withVersjon(gammelAktivitet.versjon)
            .withOppfolgingsperiodeId(oppfolgingsperiode)
            .withOpprettetDato(gammelAktivitet.opprettetDato)
            .withFhoId(gammelAktivitet.fhoId)

        ferdigstillFhoVarselHvisAktivitetFerdig(gammelAktivitet, aktivitetsData)
        leggTilIIdMappingHvisIkkeFinnes(aktivitetsData, bestilling)
        return aktivitetService.oppdaterAktivitet(gammelAktivitet, aktivitetsData)
    }

    fun ferdigstillFhoVarselHvisAktivitetFerdig(gammelAktivitet: AktivitetData, aktivitetsData: AktivitetData) {
        if (garOverIFerdigStatus(gammelAktivitet, aktivitetsData)) {
            gammelAktivitet.fhoId?.let { avtaltMedNavService.settVarselFerdig(it) }
        }
    }
    fun leggTilIIdMappingHvisIkkeFinnes(aktivitetsData: AktivitetData, bestilling: ArenaAktivitetskortBestilling) {
        val aktivitetIder = idMappingDAO.getMappingsByFunksjonellId(listOf(bestilling.aktivitetskort.id))
        if (aktivitetIder[bestilling.aktivitetskort.id] == null) {
            val idMapping = IdMapping(
                bestilling.eksternReferanseId,
                aktivitetsData.id,
                bestilling.aktivitetskort.id,
            )
            idMappingDAO.insert(idMapping)
        }
    }
    private fun ArenaAktivitetskortBestilling.finnOpprettelseType(): OpprettelseType {
        val ideer = idMappingDAO.getAktivitetIder(this.eksternReferanseId).map { it.funksjonellId }
        return when {
            ideer.contains(this.aktivitetskort.id) -> OpprettelseType.ER_MIGRERT
            ideer.isEmpty() -> OpprettelseType.IKKE_MIGRERT
            else -> OpprettelseType.ER_MIGRERT_NY_PERIODE_SPLITT
        }

    }
}

fun garOverIFerdigStatus(gammelAktivitet: AktivitetData, aktivitetsData: AktivitetData) = !gammelAktivitet.erIFerdigStatus() && aktivitetsData.erIFerdigStatus()
fun AktivitetData.erIFerdigStatus() = listOf(AktivitetStatus.AVBRUTT, AktivitetStatus.FULLFORT).contains(this.status)
fun ArenaAktivitetskortBestilling.idMapping(aktivitetId: Long) = IdMapping(
    this.eksternReferanseId,
    aktivitetId,
    this.aktivitetskort.id
)
enum class OpprettelseType {
    ER_MIGRERT,
    ER_MIGRERT_NY_PERIODE_SPLITT,
    IKKE_MIGRERT
}