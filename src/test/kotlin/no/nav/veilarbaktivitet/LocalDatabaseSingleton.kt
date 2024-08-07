package no.nav.veilarbaktivitet

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import no.nav.veilarbaktivitet.db.DbTestUtils

object LocalDatabaseSingleton {
    val postgres = EmbeddedPostgres.start().postgresDatabase.also {
        DbTestUtils.initDb(it)
    }
}