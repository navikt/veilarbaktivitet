package no.nav.veilarbaktivitet.kvp

import no.nav.veilarbaktivitet.person.Person
import org.springframework.stereotype.Service
import java.util.Date

@Service
class KvpHendelseDedupService(
    private val dedupDAO: KvpHendelseDedupDAO
) {
    fun reserverAvsluttetHendelse(aktorId: Person.AktorId, avsluttetDato: Date): Boolean {
        return dedupDAO.reserverDersomNy(lagAvsluttetDedupKey(aktorId, avsluttetDato))
    }

    fun reserverStartetHendelse(aktorId: Person.AktorId, startetDato: Date): Boolean {
        return dedupDAO.reserverDersomNy(lagStartetDedupKey(aktorId, startetDato))
    }

    private fun lagAvsluttetDedupKey(aktorId: Person.AktorId, avsluttetDato: Date): String {
        return "AVSLUTTET:${aktorId.get()}:${avsluttetDato.toInstant().toEpochMilli()}"
    }

    private fun lagStartetDedupKey(aktorId: Person.AktorId, startetDato: Date): String {
        return "STARTET:${aktorId.get()}:${startetDato.toInstant().toEpochMilli()}"
    }
}
