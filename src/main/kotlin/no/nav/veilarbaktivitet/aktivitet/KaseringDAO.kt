package no.nav.veilarbaktivitet.aktivitet

import lombok.RequiredArgsConstructor
import no.nav.veilarbaktivitet.person.Person
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@RequiredArgsConstructor
open class KasseringDAO (
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {

    @Transactional
    open fun kasserAktivitet(aktivitetId: Long, navIdent: Person.NavIdent ): Boolean {
        val params = MapSqlParameterSource().addValue("aktivitetId", aktivitetId).addValue("navIdent", navIdent.get())
        // language=sql
        val whereClause = "aktivitet_id = :aktivitetId"
        // language=sql

        return listOf(
            "UPDATE EGENAKTIVITET SET HENSIKT = 'Kassert av NAV', OPPFOLGING = 'Kassert av NAV' WHERE",
            "UPDATE STILLINGSSOK SET ARBEIDSGIVER = 'Kassert av NAV', STILLINGSTITTEL = 'Kassert av NAV', KONTAKTPERSON = 'Kassert av NAV', ETIKETT = null, ARBEIDSSTED = 'Kassert av NAV' WHERE",
            "UPDATE SOKEAVTALE SET ANTALL_STILLINGER_SOKES = 0, ANTALL_STILLINGER_I_UKEN = 0, AVTALE_OPPFOLGING = 'Kassert av NAV' WHERE",
            "UPDATE IJOBB SET ANSETTELSESFORHOLD = 'Kassert av NAV', ARBEIDSTID = 'Kassert av NAV' WHERE",
            "UPDATE BEHANDLING SET BEHANDLING_STED = 'Kassert av NAV', EFFEKT = 'Kassert av NAV', BEHANDLING_OPPFOLGING = 'Kassert av NAV', BEHANDLING_TYPE = 'Kassert av NAV' WHERE",
            "UPDATE MOTE SET ADRESSE = 'Kassert av NAV', FORBEREDELSER = 'Kassert av NAV' WHERE",
            "UPDATE MOTE SET REFERAT = 'Kassert av NAV' WHERE REFERAT IS NOT NULL AND",  // Hvis referat er satt og ikke delt, kommer det en 'ikke delt' label i aktivitetsplan
            "UPDATE STILLING_FRA_NAV SET KONTAKTPERSON_NAVN = 'Kassert av NAV', KONTAKTPERSON_TITTEL = 'Kassert av NAV', KONTAKTPERSON_MOBIL = 'Kassert av NAV', ARBEIDSGIVER = 'Kassert av NAV', ARBEIDSSTED = 'Kassert av NAV', STILLINGSID = 'kassertAvNav', SOKNADSSTATUS = null WHERE",
            "UPDATE AKTIVITET SET TITTEL = 'Det var skrevet noe feil, og det er nå slettet', AVSLUTTET_KOMMENTAR = 'Kassert av NAV', LENKE = 'Kassert av NAV', BESKRIVELSE = 'Kassert av NAV', ENDRET_AV = :navIdent, LAGT_INN_AV = 'NAV' WHERE",
            "UPDATE EKSTERNAKTIVITET SET OPPGAVE = null, HANDLINGER = null, DETALJER = null, ETIKETTER = null WHERE"
        )
            .map { sql: String -> "$sql $whereClause" }
            .sumOf { sql: String -> namedParameterJdbcTemplate.update(sql, params) } > 0

    }

    @Transactional
    open fun kasserAktivitetMedBegrunnelse(aktivitetId: Long, begrunnelse: String?, navIdent : Person.NavIdent ) {
        kasserAktivitet(aktivitetId, navIdent)
        kotlin.runCatching {
            namedParameterJdbcTemplate.update(
                """
                INSERT INTO KASSERT_AKTIVITET (AKTIVITET_ID, BEGRUNNELSE) 
                VALUES (:aktivitetId, :begrunnelse)
            """.trimIndent(),
                mapOf(
                    "aktivitetId" to aktivitetId,
                    "begrunnelse" to begrunnelse
                )
            )
        }.onFailure {
            when (it) {
                is DuplicateKeyException -> return@onFailure
                else -> throw it
            }
        }
    }
}