package no.nav.veilarbaktivitet.aktivitetskort.service

import lombok.extern.slf4j.Slf4j
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.aktivitet.AktivitetService
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetIdMappingProducer
import no.nav.veilarbaktivitet.aktivitetskort.Aktivitetskort
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortMapper.toAktivitetsData
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.feil.ManglerOppfolgingsperiodeFeil
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMapping
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDAO
import no.nav.veilarbaktivitet.arena.model.ArenaId
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDAO
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukerNotifikasjonDAO
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.person.Person.AktorId
import no.nav.veilarbaktivitet.util.DateUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Slf4j
@Service
class ArenaAktivitetskortService (
    private val forhaandsorienteringDAO: ForhaandsorienteringDAO,
    private val brukerNotifikasjonDAO: BrukerNotifikasjonDAO,
    private val idMappingDAO: IdMappingDAO,
    private val aktivitetService: AktivitetService,
    private val aktivitetDao: AktivitetDAO,
    private val oppfolgingsperiodeService: OppfolgingsperiodeService,
    private val aktivitetIdMappingProducer: AktivitetIdMappingProducer,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    fun opprettAktivitet(bestilling: ArenaAktivitetskortBestilling): AktivitetData? {
        val aktorId = bestilling.aktorId
        val opprettetTidspunkt = bestilling.aktivitetskort.endretTidspunkt.toLocalDateTime()
        val endretAv = bestilling.aktivitetskort.endretAv
        // Fant ingen passende oppfølgingsperiode - ignorerer meldingen
        val oppfolgingsperiode = oppfolgingsperiodeService.finnOppfolgingsperiode(aktorId, opprettetTidspunkt)
            ?: throw ManglerOppfolgingsperiodeFeil()

        // Opprett via AktivitetService
        val aktivitetsData = bestilling.toAktivitetsData(opprettetTidspunkt, oppfolgingsperiode)
        val opprettetAktivitetsData = aktivitetService.opprettAktivitet(
            aktorId,
            aktivitetsData,
            endretAv,
            opprettetTidspunkt,
            oppfolgingsperiode?.uuid
        )

        // Gjør arena-spesifikk migrering
        arenaspesifikkMigrering(bestilling.aktivitetskort, opprettetAktivitetsData, bestilling.eksternReferanseId)
        return opprettetAktivitetsData
    }

    fun oppdater(aktivitetsData: AktivitetData, aktorId: AktorId, aktivitetsId: Long): AktivitetData {
        val oppfolgingsperiode = oppfolgingsperiodeService.finnOppfolgingsperiode(aktorId, DateUtils.dateToLocalDateTime(aktivitetsData.opprettetDato))
            ?: throw ManglerOppfolgingsperiodeFeil()
        return aktivitetDao.overskrivAktivitet(aktivitetsData
            .withOppfolgingsperiodeId(oppfolgingsperiode.uuid)
            .withId(aktivitetsId)
        )
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
                    opprettetAktivitet.withFhoId(fho.id),
                    Person.navIdent(fho.opprettetAv),
                    DateUtils.dateToLocalDateTime(fho.opprettetDato)
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
