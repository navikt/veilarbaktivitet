package no.nav.veilarbaktivitet.brukernotifikasjon.kvitering;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.brukernotifikasjon.Brukernotifikasjon;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselStatus;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.util.EnumUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class KvitteringDAO {
    private final NamedParameterJdbcTemplate jdbc;

    RowMapper<Brukernotifikasjon> rowmapper = (rs, rowNum) ->
            Brukernotifikasjon.builder()
                    .id(rs.getLong("ID"))
                    .brukernotifikasjonId(rs.getString("BRUKERNOTIFIKASJON_ID"))
                    .aktivitetId(rs.getLong("AKTIVITET_ID"))
                    .opprettetPaaAktivitetVersjon(rs.getLong("OPPRETTET_PAA_AKTIVITET_VERSION"))
                    .foedselsnummer(rs.getString("FOEDSELSNUMMER"))
                    .oppfolgingsperiode(rs.getString("OPPFOLGINGSPERIODE"))
                    .type(EnumUtils.valueOf(VarselType.class, rs.getString("TYPE")))
                    .status(EnumUtils.valueOf(VarselStatus.class, rs.getString("STATUS")))
                    .opprettet(Database.hentDato(rs, "OPPRETTET"))
                    .melding(rs.getString("MELDING"))
                    .varselFeilet(Database.hentDato(rs, "VARSEL_FEILET"))
                    .avsluttet(Database.hentDato(rs, "AVSLUTTET"))
                    .bekreftetSendt(Database.hentDato(rs, "BEKREFTET_SENDT"))
                    .forsoktSendt(Database.hentDato(rs, "FORSOKT_SENDT"))
                    .ferdigBehandlet(Database.hentDato(rs, "FERDIG_BEHANDLET"))
                    .build();

    public void setFeilet(String bestillingsId) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("brukernotifikasjonId", bestillingsId)
                .addValue("status", VarselStatus.FEILET.toString());
        jdbc.update("update BRUKERNOTIFIKASJON set VARSEL_FEILET = current_timestamp, STATUS = :status where BRUKERNOTIFIKASJON_ID = :brukernotifikasjonId ", param);
    }

    public void setFullfortForGyldige(String bestillingsId) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("brukernotifikasjonId", bestillingsId)
                .addValue("status", VarselStatus.SENDT_OK.toString());

        jdbc.update("" +
                        " update BRUKERNOTIFIKASJON " +
                        " set" +
                        " BEKREFTET_SENDT = CURRENT_TIMESTAMP, " +
                        " STATUS = :status" +
                        " where VARSEL_FEILET is null " +
                        " and AVSLUTTET is null " +
                        " and BRUKERNOTIFIKASJON_ID = :brukernotifikasjonId"
                , param
        );
    }

    public void setFerdigBehandlet(long id) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("id", id);

        int update = jdbc.update("" +
                        " update BRUKERNOTIFIKASJON " +
                        " set FERDIG_BEHANDLET = CURRENT_TIMESTAMP " +
                        " where id = :id "
                , param);

        Assert.isTrue(update == 1, "Forventet en rad oppdatert, id=" + id);
    }

    public List<Brukernotifikasjon> hentFullfortIkkeBehandlet(int maksAntall, VarselType type) {
        // language=SQL
        String sql = "SELECT * FROM BRUKERNOTIFIKASJON" +
                " WHERE FERDIG_BEHANDLET IS NULL" +
                " AND BEKREFTET_SENDT IS NOT NULL" +
                " AND TYPE = :type" +
                " FETCH FIRST :limit ROWS ONLY";

        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("type", type.name())
                .addValue("limit", maksAntall);

        return jdbc.query(sql, parameterSource, rowmapper);
    }

    public List<Brukernotifikasjon> hentFeiletIkkeBehandlet(int maksAntall, VarselType type) {
        // language=SQL
        String sql = "SELECT * FROM BRUKERNOTIFIKASJON" +
                " WHERE FERDIG_BEHANDLET IS NULL" +
                " AND BEKREFTET_SENDT IS NULL" +
                " AND STATUS = 'FEILET'" +
                " AND TYPE = :type" +
                " FETCH FIRST :limit ROWS ONLY";

        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("type", type.name())
                .addValue("limit", maksAntall);

        return jdbc.query(sql, parameterSource, rowmapper);
    }

}
