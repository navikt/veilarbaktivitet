package no.nav.veilarbaktivitet.aktivitetskort.service

import lombok.extern.slf4j.Slf4j
import no.nav.veilarbaktivitet.aktivitet.AktivitetService
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetIdMappingProducer
import no.nav.veilarbaktivitet.aktivitetskort.Aktivitetskort
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortMapper.toAktivitetsDataInsert
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.feil.ManglerOppfolgingsperiodeFeil
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMapping
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDAO
import no.nav.veilarbaktivitet.arena.model.ArenaId
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
    private val oppfolgingsperiodeService: OppfolgingsperiodeService,
    private val aktivitetIdMappingProducer: AktivitetIdMappingProducer,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    fun opprettAktivitet(bestilling: ArenaAktivitetskortBestilling): AktivitetData? {
        val aktorId = bestilling.aktorId
        val opprettetTidspunkt = bestilling.aktivitetskort.endretTidspunkt
        // Fant ingen passende oppfølgingsperiode - ignorerer meldingen
        val oppfolgingsperiode = oppfolgingsperiodeService.finnOppfolgingsperiode(aktorId, opprettetTidspunkt.toLocalDateTime())
            ?: throw ManglerOppfolgingsperiodeFeil()

        // Opprett via AktivitetService
        val aktivitetsData = bestilling.toAktivitetsDataInsert(opprettetTidspunkt, oppfolgingsperiode.sluttDato)
        val opprettetAktivitetsData = aktivitetService.opprettAktivitet(
            aktivitetsData.withOppfolgingsperiodeId(oppfolgingsperiode.uuid)
        )

        // Gjør arena-spesifikk migrering
        arenaspesifikkMigrering(bestilling.aktivitetskort, opprettetAktivitetsData, bestilling.eksternReferanseId)
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
            aktivitetskort.id
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
}
