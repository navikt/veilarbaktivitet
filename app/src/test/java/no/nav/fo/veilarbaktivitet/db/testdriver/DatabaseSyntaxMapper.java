package no.nav.fo.veilarbaktivitet.db.testdriver;

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
        return syntaxMap.getOrDefault(sql, sql);
    }

}
