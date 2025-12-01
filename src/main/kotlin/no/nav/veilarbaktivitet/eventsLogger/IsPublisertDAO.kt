package no.nav.veilarbaktivitet.eventsLogger

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
open class IsPublisertDAO(
    val template: JdbcTemplate
) {
    open fun hentIsPublisertFordeling(): SamtalereferatPublisertFordeling {
        val sql = """
            select count(*) as antall, mote.referat_publisert as erPublisert from mote
                join veilarbaktivitet.aktivitet a
                    on mote.aktivitet_id = a.aktivitet_id and mote.versjon = a.versjon
            where mote.referat is not null
              and a.livslopstatus_kode != 'AVBRUTT'
              and a.fra_dato < NOW() -- Kun se på møter/samtalereferater som ikke er datert frem i tid
              and a.gjeldende =  1 -- Bare siste versjon
              and a.historisk_dato is null -- Ikke ta med referat i avsluttede perioder
            group by mote.referat_publisert
        """.trimIndent()
        return template.query(sql) { result, _ ->
            val erPublisert = result.getInt("erPublisert")
            val antall = result.getInt("antall")
            if (erPublisert == 1) SamtalereferatPublisertFordeling(antallPublisert = antall, antallIkkePublisert = 0)
            else SamtalereferatPublisertFordeling(antallPublisert = 0, antallIkkePublisert = antall)
        }.let { fordelinger ->
            SamtalereferatPublisertFordeling(
                antallPublisert = fordelinger.sumOf { it.antallPublisert },
                antallIkkePublisert = fordelinger.sumOf { it.antallIkkePublisert },
            )
        }
    }
}
