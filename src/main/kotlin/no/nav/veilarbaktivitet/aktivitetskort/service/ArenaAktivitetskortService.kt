package no.nav.veilarbaktivitet.aktivitetskort.service

import lombok.extern.slf4j.Slf4j
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.aktivitet.AktivitetService
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetIdMappingProducer
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortMapper.toAktivitetsDataInsert
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
    private val aktivitetDAO: AktivitetDAO,
    private val aktivitetService: AktivitetService,
    private val aktivitetIdMappingProducer: AktivitetIdMappingProducer,
    private val avtaltMedNavService: AvtaltMedNavService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    fun opprettAktivitet(bestilling: ArenaAktivitetskortBestilling): AktivitetData? {
        val opprettetTidspunkt = bestilling.aktivitetskort.endretTidspunkt

        // Opprett via AktivitetService
        val aktivitetsData = bestilling.toAktivitetsDataInsert(opprettetTidspunkt, bestilling.oppfolgingsperiodeSlutt)
        val opprettetAktivitetsData = aktivitetService.opprettAktivitet(
            aktivitetsData.withOppfolgingsperiodeId(bestilling.oppfolgingsperiode)
        )

        // Gjør arena-spesifikk migrering
        val erMigrertAllerede = idMappingDAO.getAktivitetId(bestilling.eksternReferanseId).isPresent
        if (!erMigrertAllerede) {
            arenaspesifikkMigrering(bestilling.aktivitetskort, opprettetAktivitetsData, bestilling.eksternReferanseId)
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

    fun dobbelsjekkMigrering(
        bestilling: ArenaAktivitetskortBestilling,
        opprettetAktivitet: AktivitetData): Boolean {
        val erMigrertAllerede = idMappingDAO.getAktivitetId(bestilling.eksternReferanseId).isPresent
        if (erMigrertAllerede) return false
        arenaspesifikkMigrering(bestilling.aktivitetskort, opprettetAktivitet, bestilling.eksternReferanseId)
        return true
    }

//    fun oppdaterAktivitet(
//        bestilling: ArenaAktivitetskortBestilling,
//        gammelAktivitet: AktivitetData
//    ): AktivitetData {
//        val historiskTidspunkt = gammelAktivitet.eksternAktivitetData.oppfolgingsperiodeSlutt
//        val opprettetDato = DateUtils.dateToZonedDateTime(gammelAktivitet.opprettetDato)
//        val oppfolgingsperiode = bestilling.oppfolgingsperiode
//        val aktivitetsData = bestilling.toAktivitet(opprettetDato, historiskTidspunkt
//            ?.let { ZonedDateTime.of(it, ZoneOffset.UTC) } )
//            .withId(gammelAktivitet.id)
//            .withTransaksjonsType(AktivitetTransaksjonsType.OPPRETTET)
//            .withVersjon(gammelAktivitet.versjon)
//            .withOppfolgingsperiodeId(oppfolgingsperiode)
//            .withOpprettetDato(gammelAktivitet.opprettetDato)
//            .withFhoId(gammelAktivitet.fhoId)
//
//        val ferdigstatus = listOf(AktivitetStatus.AVBRUTT, AktivitetStatus.FULLFORT)
//        if (!ferdigstatus.contains(gammelAktivitet.status) && ferdigstatus.contains(aktivitetsData.status)) {
//            gammelAktivitet.fhoId?.let { avtaltMedNavService.settVarselFerdig(it) }
//        }
//
//        val opprettetAktivitetsData = aktivitetDAO.overskrivMenMedNyVersjon(aktivitetsData)
//        return opprettetAktivitetsData
//    }
}
