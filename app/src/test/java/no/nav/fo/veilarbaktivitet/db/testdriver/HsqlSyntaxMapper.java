package no.nav.fo.veilarbaktivitet.db.testdriver;

import java.util.HashMap;
import java.util.Map;

/*
"Fattigmanns-løsning" for å kunne bruke hsql lokalt med oracle syntax
*/
class HsqlSyntaxMapper {

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
        map(
            "ALTER TABLE SOKEAVTALE RENAME COLUMN ANTALL TO ANTALL_STILLINGER_SOKES",
            "ALTER TABLE SOKEAVTALE ALTER COLUMN ANTALL RENAME TO ANTALL_STILLINGER_SOKES"
        );
        map(
            "ALTER TABLE AKTIVITET RENAME COLUMN TYPE TO AKTIVITET_TYPE_KODE",
            "ALTER TABLE AKTIVITET ALTER COLUMN TYPE RENAME TO AKTIVITET_TYPE_KODE"
        );
        map(
           "ALTER TABLE AKTIVITET RENAME COLUMN STATUS TO LIVSLOPSTATUS_KODE",
            "ALTER TABLE AKTIVITET ALTER COLUMN STATUS RENAME TO LIVSLOPSTATUS_KODE"
        );
    }

    private static void map(String oracleSyntax, String hsqlSyntax) {
        syntaxMap.put(oracleSyntax, hsqlSyntax);
    }

    static String hsqlSyntax(String sql) {
        return syntaxMap.getOrDefault(sql, sql);
    }

}
