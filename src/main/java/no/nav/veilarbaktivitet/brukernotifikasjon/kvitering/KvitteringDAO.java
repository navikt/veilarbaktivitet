package no.nav.veilarbaktivitet.brukernotifikasjon.kvitering;

import lombok.RequiredArgsConstructor;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAktivitetIder;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
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

    RowMapper<BrukernotifikasjonAktivitetIder> rowmapper = (rs, rowNum) ->
            BrukernotifikasjonAktivitetIder.builder()
                    .id(rs.getLong("ID"))
                    .aktivitetId(rs.getLong("AKTIVITET_ID"))
                    .build();

    public void setFeilet(String bestillingsId) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("brukernotifikasjonId", bestillingsId)
                .addValue("varselKvitteringStatus", VarselKvitteringStatus.FEILET.toString());
        jdbc.update("" +
                " update BRUKERNOTIFIKASJON " +
                " set VARSEL_FEILET = current_timestamp, VARSEL_KVITTERING_STATUS = :varselKvitteringStatus " +
                " where BRUKERNOTIFIKASJON_ID = :brukernotifikasjonId ", param);
    }

    public void setFullfortForGyldige(String bestillingsId) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("brukernotifikasjonId", bestillingsId)
                .addValue("varselKvitteringStatus", VarselKvitteringStatus.OK.toString());

        jdbc.update("" +
                        " update BRUKERNOTIFIKASJON " +
                        " set" +
                        " BEKREFTET_SENDT = CURRENT_TIMESTAMP, " +
                        " VARSEL_KVITTERING_STATUS = :varselKvitteringStatus" +
                        " where BRUKERNOTIFIKASJON.VARSEL_KVITTERING_STATUS != 'FEILET' " +
                        " and STATUS != 'AVSLUTTET'" +
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

    public List<BrukernotifikasjonAktivitetIder> hentFullfortIkkeBehandletForAktiviteter(int maksAntall, VarselType type) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("type", type.name())
                .addValue("limit", maksAntall);

        return jdbc.query(
                """
                        SELECT id, ab.AKTIVITET_ID FROM BRUKERNOTIFIKASJON
                        inner join AKTIVITET_BRUKERNOTIFIKASJON ab on BRUKERNOTIFIKASJON.ID = ab.BRUKERNOTIFIKASJON_ID
                         WHERE FERDIG_BEHANDLET IS NULL
                         AND VARSEL_KVITTERING_STATUS = 'OK'
                         AND TYPE = :type
                         FETCH FIRST :limit ROWS ONLY
                        """, parameterSource, rowmapper);
    }

    public List<BrukernotifikasjonAktivitetIder> hentFeiletIkkeBehandlet(int maksAntall, VarselType type) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("type", type.name())
                .addValue("limit", maksAntall);

        return jdbc.query("""
                SELECT id, ab.AKTIVITET_ID FROM BRUKERNOTIFIKASJON
                inner join AKTIVITET_BRUKERNOTIFIKASJON ab on BRUKERNOTIFIKASJON.ID = ab.BRUKERNOTIFIKASJON_ID
                 WHERE FERDIG_BEHANDLET IS NULL
                 AND VARSEL_KVITTERING_STATUS = 'FEILET'
                 AND TYPE = :type
                 FETCH FIRST :limit ROWS ONLY
                """, parameterSource, rowmapper);
    }

    public void lagreKvitering(String bestillingsId, DoknotifikasjonStatus melding) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("BRUKERNOTIFIKASJON_ID", bestillingsId)
                .addValue("STATUS", melding.getStatus())
                .addValue("MELDING", melding.getMelding())
                .addValue("distribusjonId", melding.getDistribusjonId())
                .addValue("BESKJED",melding.toString());
        jdbc.update("""
                insert into  BRUKERNOTIFIKAJSON_KVITERING_TABELL
                        (  BRUKERNOTIFIKASJON_ID,  STATUS,  MELDING,  distribusjonId,  BESKJED )
                VALUES  ( :BRUKERNOTIFIKASJON_ID, :STATUS, :MELDING, :distribusjonId, :BESKJED )
                """, parameterSource);
    }
}
