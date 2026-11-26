package no.nav.veilarbaktivitet.motesms;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.MinSideVarselId;
import no.nav.veilarbaktivitet.veilarbdbutil.VeilarbAktivitetSqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
public class MoteSmsDAO {
    private final NamedParameterJdbcTemplate jdbc;

    List<Long> hentMoteSmsSomFantStedForMerEnd(Duration duration) {

        SqlParameterSource params = new VeilarbAktivitetSqlParameterSource()
                .addValue("eldreEnd", ZonedDateTime.now().minus(duration));
        return jdbc.queryForList("""
                        select AKTIVITET_ID from GJELDENDE_MOTE_SMS
                        where BRUKERNOTIFIKASJON = 1
                        and MOTETID < :eldreEnd
                        """,
                params,
                long.class);
    }

    record AktivitetVersjon(long aktivitetId, long versjon) {
    }
    List<AktivitetVersjon> hentMoterMedOppdatertTidEllerKanal(int max, long versjonOffset) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", max)
                .addValue("versjonOffset", versjonOffset);
        return jdbc.query(
                """
                        select AKTIVITET.AKTIVITET_ID, AKTIVITET.VERSJON from AKTIVITET
                        inner join MOTE on AKTIVITET.AKTIVITET_ID = MOTE.AKTIVITET_ID and AKTIVITET.VERSJON = MOTE.VERSJON
                        inner join GJELDENDE_MOTE_SMS SMS on AKTIVITET.AKTIVITET_ID = SMS.AKTIVITET_ID
                        where GJELDENDE = 1
                        and AKTIVITET.VERSJON > :versjonOffset
                        and (SMS.MOTETID != AKTIVITET.FRA_DATO or SMS.KANAL != MOTE.KANAL)
                        limit :limit
                        """
                , params, (rs, rowNum) -> new AktivitetVersjon(rs.getLong("AKTIVITET_ID"), rs.getLong("VERSJON")));
    }

    void slettGjeldende(long aktivitetId) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktivit_id", aktivitetId);
        jdbc.update("delete from  GJELDENDE_MOTE_SMS where AKTIVITET_ID = :aktivit_id", params);
    }

    void insertGjeldendeSms(MoteNotifikasjon moteNotifikasjon, MinSideVarselId varselId) {
        var varselIdParam = varselId != null ? varselId.getValue() : null;
        SqlParameterSource params = new VeilarbAktivitetSqlParameterSource()
                .addValue("aktivit_id", moteNotifikasjon.aktivitetId())
                .addValue("kanal", moteNotifikasjon.kanalDTO().name())
                .addValue("moteTid", moteNotifikasjon.startTid())
                .addValue("varselId", varselIdParam);

        jdbc.update(
                """
                        insert into GJELDENDE_MOTE_SMS
                                (AKTIVITET_ID, MOTETID,  KANAL, BRUKERNOTIFIKASJON, VARSEL_ID)
                         values (:aktivit_id,  :moteTid, :kanal, 1, :varselId)
                        """
                , params
        );
    }


}
