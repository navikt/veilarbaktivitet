package no.nav.veilarbaktivitet.oppfolging.periode

import no.nav.veilarbaktivitet.person.Person.AktorId
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service

@Service
open class OppfolgingsperiodeDAO(val jdbc: NamedParameterJdbcTemplate) {

    val log = LoggerFactory.getLogger(javaClass)

    fun upsertOppfolgingsperide(oppfolgingsperiode: Oppfolgingsperiode) {
        val params = mapOf(
            "aktorId" to oppfolgingsperiode.aktorid,
            "id" to oppfolgingsperiode.oppfolgingsperiode.toString(),
            "fra" to oppfolgingsperiode.startTid,
            "til" to oppfolgingsperiode.sluttTid
        )
        jdbc.update(
            """
                merge into OPPFOLGINGSPERIODE
                using (SELECT TO_CHAR(:id) AS id from DUAL) INPUTID 
                on (INPUTID.id = OPPFOLGINGSPERIODE.id)
                when matched then 
                update set til = :til
                when not matched then
                insert (aktorId, id, fra, til) 
                values (:aktorId, :id, :fra, :til)
                """.trimIndent(), params
        )

        log.info("oppfølgingsperiodemelding behandlet {}", oppfolgingsperiode)

    }

    fun getByAktorId(aktorId: AktorId ): List<Oppfolgingsperiode> {
        val params = mapOf("aktorId" to aktorId.get())

        val sql = """
            SELECT * FROM OPPFOLGINGSPERIODE WHERE AKTORID = :aktorId ORDER BY coalesce(til, TO_DATE('9999-12-31', 'YYYY-MM-DD')) DESC
        """.trimIndent()

        jdbc.query()
        return
    }

}