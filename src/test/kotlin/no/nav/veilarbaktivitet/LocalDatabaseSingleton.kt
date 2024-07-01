package no.nav.veilarbaktivitet

import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension
import no.nav.veilarbaktivitet.db.DbTestUtils

object LocalDatabaseSingleton {
    val postgres = EmbeddedPostgresExtension.preparedDatabase {
        DbTestUtils.initDb(it)
    }
}