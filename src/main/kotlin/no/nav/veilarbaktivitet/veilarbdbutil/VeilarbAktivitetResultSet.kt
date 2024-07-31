package no.nav.veilarbaktivitet.veilarbdbutil

import java.sql.ResultSet

class VeilarbAktivitetResultSet(private val resultSet: ResultSet): ResultSet by resultSet {

    override fun getBoolean(columnLabel: String?): Boolean {
        return when(resultSet.getInt(columnLabel)) {
            0 -> false
            1 -> true
            else -> throw RuntimeException("Leser tall som boolean, fant et annet tall enn 0 og 1")
        }
    }

    fun getBooleanOrNull(columnLabel: String?): Boolean? {
        if (resultSet.getObject(columnLabel) == null) return null
        return resultSet.getBoolean(columnLabel)
    }
}