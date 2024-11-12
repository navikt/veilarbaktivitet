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
    override fun isWrapperFor( iface: Class<*>): Boolean {
        // TODO Auto-generated method stub
        return iface != null && iface.isAssignableFrom(this.javaClass)
    }
    override fun <T> unwrap( iface: Class<T>): T {
        // TODO Auto-generated method stub
        try {
            if (iface != null && iface.isAssignableFrom(this.javaClass)) {
                return this as T
            }
            throw  java.sql.SQLException("Auto-generated unwrap failed; Revisit implementation")
        } catch ( e: Exception) {
            throw  java.sql.SQLException(e)
        }
    }
}