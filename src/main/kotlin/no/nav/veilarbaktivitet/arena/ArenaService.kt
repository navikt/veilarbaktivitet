package no.nav.veilarbaktivitet.arena

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import no.nav.common.types.identer.Fnr
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper
import no.nav.veilarbaktivitet.aktivitetskort.MigreringService
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDAO
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingWithAktivitetStatus
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetTypeDTO
import no.nav.veilarbaktivitet.arena.model.ArenaId
import no.nav.veilarbaktivitet.avtalt_med_nav.AvtaltMedNavService
import no.nav.veilarbaktivitet.avtalt_med_nav.Forhaandsorientering
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDAO
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDTO
import no.nav.veilarbaktivitet.brukernotifikasjon.MinsideVarselService
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.ArenaAktivitetVarsel
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeDAO
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.person.PersonService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
open class ArenaService(
    private val fhoDAO: ForhaandsorienteringDAO,
    private val meterRegistry: MeterRegistry,
    private val brukernotifikasjonArenaAktivitetService: MinsideVarselService,
    private val veilarbarenaClient: VeilarbarenaClient,
    private val idMappingDAO: IdMappingDAO,
    private val personService: PersonService,
    private val aktivitetDAO: AktivitetDAO,
    private val oppfolgingsperiodeDAO: OppfolgingsperiodeDAO,
    private val migreringService: MigreringService
) {
    init {
        Counter.builder(AVTALT_MED_NAV_COUNTER)
            .description("Antall arena aktiviteter som er avtalt med NAV")
            .tags(AKTIVITET_TYPE_LABEL, "", FORHAANDSORIENTERING_TYPE_LABEL, "")
            .register(meterRegistry)
    }

    val log = LoggerFactory.getLogger(javaClass)

    open fun hentAktiviteterRaw(fnr: Person.Fnr?): List<ArenaAktivitetDTO> {
        val aktiviteterFraArena = veilarbarenaClient.hentAktiviteter(fnr)
        if (aktiviteterFraArena.isEmpty) return emptyList()

        val aktorId = personService.getAktorIdForPersonBruker(fnr)
            .orElseThrow {
                ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Fant ikke aktorId"
                )
            }

        val oppfolgingsperioder = oppfolgingsperiodeDAO.getByAktorId(aktorId)
        val aktiviteter = VeilarbarenaMapper.map(aktiviteterFraArena.get(), oppfolgingsperioder)

        val forhaandsorienteringData = fhoDAO.getAlleArenaFHO(aktorId)

        return aktiviteter.map(mergeMedForhaandsorientering(forhaandsorienteringData)).toList()
    }

    open fun hentAktivitet(ident: Person.Fnr?, aktivitetId: ArenaId): Optional<ArenaAktivitetDTO> {
        return hentAktiviteterRaw(ident).stream()
            .filter { arenaAktivitetDTO: ArenaAktivitetDTO -> aktivitetId.id() == arenaAktivitetDTO.id }
            .findAny()
    }

    open fun mergeMedForhaandsorientering(forhaandsorienteringData: List<Forhaandsorientering>): (a: ArenaAktivitetDTO) -> ArenaAktivitetDTO {
        return { arenaAktivitetDTO: ArenaAktivitetDTO ->
            arenaAktivitetDTO.setForhaandsorientering(
                forhaandsorienteringData
                    .stream()
                    .filter { arenaForhaandsorienteringData: Forhaandsorientering -> arenaForhaandsorienteringData.arenaAktivitetId == arenaAktivitetDTO.id }
                    .max(Comparator.comparing { obj: Forhaandsorientering -> obj.opprettetDato })
                    .map { forhaandsorientering: Forhaandsorientering? ->
                        AktivitetDTOMapper.mapForhaandsorientering(
                            forhaandsorientering
                        )
                    }
                    .orElse(null)
            )
        }
    }

    @Transactional
    @Throws(ResponseStatusException::class)
    open fun opprettFHO(
        arenaaktivitetId: ArenaId,
        fnr: Person.Fnr,
        forhaandsorientering: ForhaandsorienteringDTO,
        opprettetAv: String
    ): ArenaAktivitetDTO {
        val arenaAktivitetDTO = hentAktivitet(fnr, arenaaktivitetId)
            .orElseThrow {
                ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Aktiviteten finnes ikke"
                )
            }

        val aktorId = personService.getAktorIdForPersonBruker(fnr)
            .orElseThrow {
                ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Fant ikke aktorId"
                )
            }

        val fho = fhoDAO.getFhoForArenaAktivitet(arenaaktivitetId)

        if (fho != null) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Det er allerede sendt forhaandsorientering på aktiviteten"
            )
        }

        if (!brukernotifikasjonArenaAktivitetService.kanVarsles(aktorId)) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Bruker kan ikke varsles")
        }

        val aktivitetId = idMappingDAO.getLatestAktivitetsId(arenaaktivitetId)
        brukernotifikasjonArenaAktivitetService.opprettVarselPaaArenaAktivitet(
            ArenaAktivitetVarsel(
                arenaaktivitetId,
                aktivitetId,
                fnr,
                AvtaltMedNavService.FORHAANDSORIENTERING_DITT_NAV_TEKST
            )
        )

        val nyForhaandsorientering = fhoDAO.insertForArenaAktivitet(
            forhaandsorientering,
            arenaaktivitetId,
            aktorId,
            opprettetAv,
            Date(),
            aktivitetId
        )
        meterRegistry.counter(
            AVTALT_MED_NAV_COUNTER,
            FORHAANDSORIENTERING_TYPE_LABEL,
            forhaandsorientering.type.name,
            AKTIVITET_TYPE_LABEL,
            arenaAktivitetDTO.type.name
        ).increment()
        return arenaAktivitetDTO.setForhaandsorientering(nyForhaandsorientering.toDTO())
    }

    open fun hentArenaAktiviteter(fnr: Person.Fnr): List<ArenaAktivitetDTO> {

        val arenaAktiviteter = hentAktiviteterRaw(fnr)

        // Id-er og versjoner
        val ideer = arenaAktiviteter.map { arenaAktivitetDTO: ArenaAktivitetDTO -> ArenaId(arenaAktivitetDTO.id) }
        val idMappings = idMappingDAO.getMappings(ideer)
        val sisteIdMappinger = idMappingDAO.onlyLatestMappings(idMappings)
        val aktivitetsVersjoner = aktivitetDAO.getAktivitetsVersjoner(
            sisteIdMappinger.values.map(IdMappingWithAktivitetStatus::aktivitetId)
        )

        // Metrikker
        migreringService.countArenaAktiviteter(
            arenaAktiviteter,
            sisteIdMappinger
        )

        val skalFiltreresBort = migreringService.filtrerBortArenaTiltakHvisToggleAktiv(idMappings.keys)
        val filtrerteArenaAktiviteter = arenaAktiviteter
            // Bare vis arena aktiviteter som mangler id, dvs ikke er migrert
            .filter { skalFiltreresBort(it) }
            .filter { it.oppfolgingsperiodeId != null }
            .map { arenaAktivitet: ArenaAktivitetDTO ->
                val idMapping = sisteIdMappinger[ArenaId(arenaAktivitet.id)]
                if (idMapping != null) return@map arenaAktivitet
                    .withId(idMapping.aktivitetId.toString())
                    .withVersjon(aktivitetsVersjoner[idMapping.aktivitetId]!!)
                arenaAktivitet
            }
        logUmigrerteIder(filtrerteArenaAktiviteter)
        loggArenaTiltakUtenOppfolging(arenaAktiviteter)
        val filtrerteArenaAktiviteterUtenTiltaksaktivitet = filtrerteArenaAktiviteter.filter { it.type != ArenaAktivitetTypeDTO.TILTAKSAKTIVITET }
        return filtrerteArenaAktiviteterUtenTiltaksaktivitet
    }

    open fun logUmigrerteIder(arenaAktiviteter: List<ArenaAktivitetDTO>) {
        val umigrerteTiltaksIder = arenaAktiviteter
            .filter { aktivitet: ArenaAktivitetDTO -> aktivitet.type == ArenaAktivitetTypeDTO.TILTAKSAKTIVITET }
            .joinToString(",") { obj: ArenaAktivitetDTO -> obj.id }
        if (umigrerteTiltaksIder.isNotEmpty()) {
            log.info("Umigrerte tiltaksIdEr: $umigrerteTiltaksIder")
        }
    }

    open fun loggArenaTiltakUtenOppfolging(aktiviteter: List<ArenaAktivitetDTO>) {
        val tiltakIdErUtenOppfolgingsperiode = aktiviteter
            .filter { it.oppfolgingsperiodeId == null }
            .joinToString(",") { it.id }
        if (tiltakIdErUtenOppfolgingsperiode.isNotEmpty()) {
            log.info("Arenaaktiviteter uten oppfolgingsperiode: $tiltakIdErUtenOppfolgingsperiode")
        }
    }

    @Transactional
    open fun markerSomLest(fnr: Fnr, aktivitetId: ArenaId): ArenaAktivitetDTO {
        val fho = fhoDAO.getFhoForArenaAktivitet(aktivitetId)

        if (fho == null) {
            log.warn("Kan ikke markere forhåndsorientering som lest. Fant ikke forhåndsorientering for aktivitet med id:$aktivitetId")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Forhåndsorientering finnes ikke")
        }

        if (fho.lestDato != null) {
            log.warn("Kan ikke markere forhåndsorientering som lest. Forhåndsorienteringen for aktivitet med id:$aktivitetId er allerede lest")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Forhåndsorienteringen er allerede lest")
        }

        brukernotifikasjonArenaAktivitetService.setDone(aktivitetId, VarselType.FORHAANDSORENTERING)

        fhoDAO.markerSomLest(fho.id, Date(), null)

        return hentAktivitet(Person.fnr(fnr.get()), aktivitetId)
            .orElseThrow {
                ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Kunne ikke hente aktiviteten"
                )
            }
    }

    companion object {
        const val AVTALT_MED_NAV_COUNTER: String = "arena.avtalt.med.nav"
        const val AKTIVITET_TYPE_LABEL: String = "AktivitetType"
        const val FORHAANDSORIENTERING_TYPE_LABEL: String = "ForhaandsorienteringType"
    }
}
