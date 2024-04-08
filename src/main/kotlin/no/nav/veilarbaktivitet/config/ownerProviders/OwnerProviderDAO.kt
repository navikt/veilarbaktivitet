package no.nav.veilarbaktivitet.config.ownerProviders

import no.nav.veilarbaktivitet.arena.model.ArenaId
import no.nav.veilarbaktivitet.person.Person.AktorId
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.*

@Repository
open class OwnerProviderDAO(private val template: NamedParameterJdbcTemplate) {

    open fun getAktivitetOwner(aktivitetId: Long): AktorId? {
        val sql = """
            SELECT AKTOR_ID FROM AKTIVITET WHERE GJELDENDE = 1 AND AKTIVITET_ID = :aktivitetId
        """.trimIndent()
        return template.query(sql, mapOf("aktivitetId" to aktivitetId))
            { row, _ -> row.getString("AKTOR_ID") }
            .firstOrNull()
            .let { AktorId.aktorId(it) }
    }

    open fun getForhaandsorienteringOwner(arenaIdSomLiggerIFHO: ArenaId): AktorId? {
        val sql = """
            SELECT AKTOR_ID FROM FORHAANDSORIENTERING WHERE ARENAAKTIVITET_ID = :arenaIdSomLiggerIFHO
        """.trimIndent()
        return template.query(sql, mapOf("arenaIdSomLiggerIFHO" to arenaIdSomLiggerIFHO))
            { row, _ -> row.getString("AKTOR_ID") }
            .firstOrNull()
            .let { AktorId.aktorId(it) }
    }

    open fun getOppfolgingsperiodeOwner(uuid: UUID): AktorId? {
        val sql = """
            SELECT AKTORID FROM OPPFOLGINGSPERIODE WHERE id = :uuid
        """.trimIndent()
        return template.query(sql, mapOf("uuid" to uuid.toString()))
            { row, _ -> row.getString("AKTORID") }
            .firstOrNull()
            .let { AktorId.aktorId(it) }
    }

}
