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
                "alter table AKTIVITET modify lob(BESKRIVELSE) (deduplicate)",
                "SELECT 1 FROM DUAL"
        );
        map(
                "alter table AKTIVITET modify lob(LENKE) (deduplicate)",
                "SELECT 1 FROM DUAL"
        );
        map(
                "alter table SOKEAVTALE modify lob(AVTALE_OPPFOLGING) (deduplicate)",
                "SELECT 1 FROM DUAL"
        );
    }

    private static void map(String oracleSyntax, String hsqlSyntax) {
        syntaxMap.put(oracleSyntax, hsqlSyntax);
    }

    static String hsqlSyntax(String sql) {
        return syntaxMap.getOrDefault(sql, sql);
    }

}
