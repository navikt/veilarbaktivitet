package no.nav.veilarbaktivitet.avtaltMedNav.varsel;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.avtaltMedNav.Type;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AvtaltVarselRepository {
    private final JdbcTemplate template;

    List<VarselIdHolder> hentVarslerSkalSendes(int limit) {
        return template.queryForList("" +
                        "select ID, AKTIVITET_ID, ARENAAKTIVITET_ID, AKTOR_ID " +
                        "from FORHAANDSORIENTERING " +
                        "where VARSEL_ID is null " +
                        "   and VARSEL_FERDIG is null " +
                        "   and TYPE != ? limit ?",
                VarselIdHolder.class, Type.IKKE_SEND_FORHAANDSORIENTERING.toString(), limit);
    }

    void markerVarselSomSendt(String id, String varselId) {
        int update = template.update("" +
                        "update FORHAANDSORIENTERING set VARSEL_ID = ? " +
                        "where ID = ? and VARSEL_ID is null",
                varselId, id);

        if (update != 1L) {
            throw new IllegalStateException("Forhaandsorientering alerede sendt");
        }
    }

    List<String> hentVarslerSomSkalStoppes(int limit) {
        return template.queryForList("" +
                "select VARSEL_ID " +
                "from FORHAANDSORIENTERING " +
                "where VARSEL_FERDIG is not null " +
                "   and FERDIG_SENDT is null limit ?",
                String.class, limit);
    }

    void markerVareslStoppetSomSendt(String varselId) {
        int update = template.update("" +
                "update FORHAANDSORIENTERING set FERDIG_SENDT = CURRENT_TIMESTAMP" +
                " where FERDIG_SENDT is null " +
                "   and VARSEL_ID = ? ", varselId);

        if (update != 1L) {
            throw new IllegalStateException("Forhaandsorentering varsel allerede stoppet");
        }
    }

}
