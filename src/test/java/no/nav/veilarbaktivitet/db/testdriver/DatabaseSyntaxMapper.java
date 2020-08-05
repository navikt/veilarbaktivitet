package no.nav.veilarbaktivitet.db.testdriver;

import java.util.HashMap;
import java.util.Map;

/*
"Fattigmanns-løsning" for å kunne bruke h2 lokalt med oracle syntax
*/
class DatabaseSyntaxMapper {

    private static final Map<String, String> syntaxMap = new HashMap<>();

    static {
        map(
                "ALTER TABLE AKTIVITET MODIFY LOB (BESKRIVELSE) ( DEDUPLICATE )",
                "SELECT 1 FROM DUAL"
        );
        map(
                "ALTER TABLE AKTIVITET MODIFY LOB (LENKE) ( DEDUPLICATE )",
                "SELECT 1 FROM DUAL"
        );
        map(
                "ALTER TABLE SOKEAVTALE MODIFY LOB (AVTALE_OPPFOLGING) ( DEDUPLICATE )",
                "SELECT 1 FROM DUAL"
        );
    }

    private static void map(String oracleSyntax, String syntax) {
        syntaxMap.put(oracleSyntax, syntax);
    }

    static String syntax(String sql) {
        String fixedSql = sql;
         /*
         Fikser en bug i Flyway som gjør at installed_by ikke blir satt i Oracle modus med H2.
         Dette har blitt fikset i en nyere versjon av Flyway, men denne versjonen støtter ikke gammle Oracle databaser som vi i dag bruker.
        */
        if (sql.contains("installed_by")) {
            fixedSql = sql.replace("\"installed_by\" VARCHAR(100) NOT NULL", "\"installed_by\" VARCHAR(100) DEFAULT 'local_test'");
        }

        return syntaxMap.getOrDefault(fixedSql, fixedSql);
    }

}
