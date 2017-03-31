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
                "alter table AKTIVITET modify status varchar(255) not null",
                "alter table AKTIVITET alter column status SET not null"
        );
        map(
                "alter table AKTIVITET modify (beskrivelse varchar(5000), lenke varchar(2000))",
                "alter table AKTIVITET alter column beskrivelse varchar(5000); alter table AKTIVITET alter column lenke varchar(2000)"
        );

    }

    private static void map(String oracleSyntax, String hsqlSyntax) {
        syntaxMap.put(oracleSyntax, hsqlSyntax);
    }

    static String hsqlSyntax(String sql) {
        return syntaxMap.getOrDefault(sql, sql);
    }

}
