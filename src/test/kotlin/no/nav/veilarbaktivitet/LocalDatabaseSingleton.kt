package no.nav.veilarbaktivitet

import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension
import no.nav.veilarbaktivitet.db.DbTestUtils

object LocalDatabaseSingleton {
    val postgres = EmbeddedPostgresExtension.preparedDatabase {
//        it.connection.schema = "veilarbaktivitet"
        it.connection.use { connection ->
            connection.prepareStatement("""
              CREATE USER veilarbaktivitet NOLOGIN;
              GRANT CONNECT on DATABASE postgres to veilarbaktivitet;
              GRANT USAGE ON SCHEMA public to veilarbaktivitet;
            """.trimIndent()).executeUpdate()
        }

        DbTestUtils.initDb(it)
    }
}