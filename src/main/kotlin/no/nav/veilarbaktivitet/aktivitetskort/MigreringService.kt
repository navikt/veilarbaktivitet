package no.nav.veilarbaktivitet.aktivitetskort

import lombok.extern.slf4j.Slf4j
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.MessageSource
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingWithAktivitetStatus
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetTypeDTO
import no.nav.veilarbaktivitet.arena.model.ArenaId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@Slf4j
class MigreringService (
    private val aktivitetskortMetrikker: AktivitetskortMetrikker
) {
    val log = LoggerFactory.getLogger(javaClass)

    /* Hører kanskje ikke til her men var lettere å gjøre groupBy i kotlin vs java */
    fun countArenaAktiviteter(
        foer: List<ArenaAktivitetDTO>,
        idMappings: Map<ArenaId, IdMappingWithAktivitetStatus>
    ) {
        if (foer.isEmpty()) return
        val antallFoer = foer.groupBy { it.type }.mapValues { it.value.size }

        // Bruker status som indikator på om dataene er riktig
        fun sjekkMigreringsStatus(aktivitet: ArenaAktivitetDTO): MigreringsStatus {
            if (aktivitet.oppfolgingsperiodeId == null) return MigreringsStatus.IkkeMigrertManglerOppfolgingsperiode
            return idMappings[ArenaId(aktivitet.id)]
                ?.let { match -> when {
                    match.source != MessageSource.ARENA_TILTAK_AKTIVITET_ACL -> MigreringsStatus.MigrertAvAnnetTeam
                    match.status == aktivitet.status -> MigreringsStatus.MigrertRiktigStatus
                    else -> MigreringsStatus.MigrertFeilStatus
                } } ?: MigreringsStatus.IkkeMigrert
        }
        val alleInklMigrertAvAndreTeam = foer
            .map { it to sjekkMigreringsStatus(it) }
        val (alleMigreringsStatuser, alleMigrertAvAndreTeam) = alleInklMigrertAvAndreTeam
            .partition { it.second != MigreringsStatus.MigrertAvAnnetTeam }
        val migrert = alleMigreringsStatuser
            .filter { listOf(MigreringsStatus.MigrertFeilStatus, MigreringsStatus.MigrertRiktigStatus).contains(it.second) }

        val (etterMedRiktigStatus, etterMedFeilStatus) = migrert.partition { it.second == MigreringsStatus.MigrertRiktigStatus }
        val antallMigrertMedRiktigStatus = etterMedRiktigStatus.groupBy { it.first.type }.mapValues { it.value.size }
        val antallMigrertMedFeilStatus = etterMedFeilStatus.groupBy { it.first.type }.mapValues { it.value.size }
        val feilStatusIdEr = etterMedFeilStatus.joinToString(",") { it.first.id }
        val ikkeMigrertManglerOppfolg = alleMigreringsStatuser
            .filter { it.second == MigreringsStatus.IkkeMigrertManglerOppfolgingsperiode }.groupBy { it.first.type }.mapValues { it.value.size }
        val antallMigrertAvAndreTeam = alleMigrertAvAndreTeam.groupBy { it.first.type }.mapValues { it.value.size }
        if (feilStatusIdEr.isNotEmpty()) {
            log.info("Migrerte aktiviteter med feil status: $feilStatusIdEr")
        }
        ArenaAktivitetTypeDTO.values()
            .map {
                val totaltFoer = antallFoer[it] ?: 0
                val totaltMigrertRiktigStatus = antallMigrertMedRiktigStatus[it] ?: 0
                val totaltMigrertFeilStatus = antallMigrertMedFeilStatus[it] ?: 0
                val ikkeMigrertManglerOppfolg = ikkeMigrertManglerOppfolg[it] ?: 0
                val ikkeMigrert = totaltFoer - (totaltMigrertRiktigStatus + totaltMigrertFeilStatus + ikkeMigrertManglerOppfolg)
                val migrertAvAndreTeam = antallMigrertAvAndreTeam[it] ?: 0

                if (antallMigrertMedFeilStatus.isNotEmpty()) {
                    log.info("Migrerte tiltak med feil status ${etterMedFeilStatus.joinToString(",") { a -> a.first.id }}")
                }

                reportMetric(it, totaltFoer, totaltMigrertRiktigStatus, totaltMigrertFeilStatus, ikkeMigrert, ikkeMigrertManglerOppfolg, migrertAvAndreTeam)
            }
    }
    private fun reportMetric(type: ArenaAktivitetTypeDTO, total: Int, migrertRiktigStatus: Int, migrertFeilStatus: Int, ikkeMigrert: Int, ikkeMigrertManglerOppfolg: Int, migrertAvAndreTeam: Int) {
        if (total == 0) return
        aktivitetskortMetrikker.countMigrerteArenaAktiviteter(type, total, migrertRiktigStatus, migrertFeilStatus, ikkeMigrert, ikkeMigrertManglerOppfolg, migrertAvAndreTeam)
    }

    fun filtrerBortArenaTiltak(arenaIds: Set<ArenaId?>): (ArenaAktivitetDTO) -> Boolean {
        // Hvis migrert, skjul fra /tiltak endepunkt
        return { arenaAktivitetDTO ->
            /* Fjern "historiserte" arena-aktiviteter ref
                * https://confluence.adeo.no/pages/viewpage.action?pageId=414017745 */
            val erHistorisertTiltak = arenaAktivitetDTO.id.startsWith("ARENATAH")
            val erMigrert = arenaIds.contains(ArenaId(arenaAktivitetDTO.id))
            val skalVises = !erHistorisertTiltak && !erMigrert
            skalVises
        }
    }
}

enum class MigreringsStatus {
    IkkeMigrert,
    MigrertRiktigStatus,
    MigrertFeilStatus,
    IkkeMigrertManglerOppfolgingsperiode,
    MigrertAvAnnetTeam
}
