package no.nav.veilarbaktivitet.aktivitetskort.service

import lombok.extern.slf4j.Slf4j
import no.nav.veilarbaktivitet.aktivitet.AktivitetService
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetIdMappingProducer
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortMapper.toAktivitetsDataInsert
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortMapper.toAktivitetsDataUpdate
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.dto.Aktivitetskort
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMapping
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDAO
import no.nav.veilarbaktivitet.arena.model.ArenaId
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

        // Gjør arena-spesifikk migrering hvis ikke migrert allerede
        val aktivitetIder = idMappingDAO.getAktivitetIder(bestilling.eksternReferanseId)
        val erMigrertAllerede = aktivitetIder.map { it.funksjonellId }.contains(bestilling.aktivitetskort.id)
        when {
            erMigrertAllerede -> {}
            aktivitetIder.isEmpty() -> {
                arenaspesifikkMigrering(bestilling.aktivitetskort, opprettetAktivitetsData, bestilling.eksternReferanseId)
            }
            else -> {
                // Aktivitet er migret men legger til splitt-kort i id-mapping
                val idMapping = IdMapping(
                    bestilling.eksternReferanseId,
                    opprettetAktivitetsData.id,
                    bestilling.aktivitetskort.id,
                )
                idMappingDAO.insert(idMapping)
            }
        }
        return opprettetAktivitetsData
    }

    private fun arenaspesifikkMigrering(
        aktivitetskort: Aktivitetskort,
        opprettetAktivitet: AktivitetData,
        arenaId: ArenaId
    ) {
        val idMapping = IdMapping(
            arenaId,
            opprettetAktivitet.getId(),
            aktivitetskort.id,
        )
        idMappingDAO.insert(idMapping)
        forhaandsorienteringDAO.getFhoForArenaAktivitet(arenaId)
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
            arenaId
        )
        // Send idmapping til dialog
        aktivitetIdMappingProducer.publishAktivitetskortIdMapping(idMapping)
    }

    fun dobbelsjekkMigrering(
        bestilling: ArenaAktivitetskortBestilling,
        opprettetAktivitet: AktivitetData): Boolean {
        val aktivitetIder = idMappingDAO.getAktivitetIder(bestilling.eksternReferanseId)
        return when {
            aktivitetIder.map { it.funksjonellId }.contains(bestilling.aktivitetskort.id) -> false
            aktivitetIder.isEmpty() -> {
                arenaspesifikkMigrering(bestilling.aktivitetskort, opprettetAktivitet, bestilling.eksternReferanseId)
                true
            }
            else -> {
                // Only insert mapping
                val idMapping = IdMapping(
                    bestilling.eksternReferanseId,
                    opprettetAktivitet.id,
                    bestilling.aktivitetskort.id,
                )
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
    private fun ArenaAktivitetskortBestilling.erMigrert(): Boolean {
        return idMappingDAO.getAktivitetIder(this.eksternReferanseId)
            .map { it.funksjonellId }.contains(this.aktivitetskort.id)
    }
}
fun garOverIFerdigStatus(gammelAktivitet: AktivitetData, aktivitetsData: AktivitetData) = !gammelAktivitet.erIFerdigStatus() && aktivitetsData.erIFerdigStatus()
fun AktivitetData.erIFerdigStatus() = listOf(AktivitetStatus.AVBRUTT, AktivitetStatus.FULLFORT).contains(this.status)