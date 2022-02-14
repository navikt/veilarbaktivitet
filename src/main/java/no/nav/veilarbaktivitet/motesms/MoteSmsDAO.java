package no.nav.veilarbaktivitet.motesms;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
public class MoteSmsDAO {
    private final NamedParameterJdbcTemplate jdbc;

    @SneakyThrows
    private static MoteNotifikasjon mapMoteNotifikasjon(ResultSet rs, int r) {
        return new MoteNotifikasjon(rs.getInt("AKTIVITET_ID"), rs.getInt("VERSJON"), Person.aktorId(rs.getString("AKTOR_ID")), KanalDTO.valueOf(rs.getString("MOTE.KANAL")), Database.hentZonedDateTime(rs, "AKTIVITET.FRA_DATO"));
    }

    List<Long> hentMoteSmsSomFantStedForMerEnd(Duration duration) {
        SqlParameterSource params = new MapSqlParameterSource("eldreEnd", ZonedDateTime.now().minus(duration));
        return jdbc.queryForList("""
                        select AKTIVITET_ID from GJELDENDE_MOTE_SMS
                        where BRUKERNOTIFIKASJON = 1
                        and MOTETID < :eldreEnd
                        """,
                params,
                long.class);
    }

    List<Long> hentMoterMedOppdatertTidEllerKanal(int max) {
        MapSqlParameterSource params = new MapSqlParameterSource("limit", max);
        return jdbc.queryForList(
                """
                        select AKTIVITET.AKTIVITET_ID from AKTIVITET
                        inner join MOTE on AKTIVITET.AKTIVITET_ID = MOTE.AKTIVITET_ID and AKTIVITET.VERSJON = MOTE.VERSJON
                        inner join GJELDENDE_MOTE_SMS SMS on AKTIVITET.AKTIVITET_ID = SMS.AKTIVITET_ID
                        where GJELDENDE = 1
                        and (SMS.MOTETID != AKTIVITET.FRA_DATO or SMS.KANAL != MOTE.KANAL)
                        FETCH NEXT :limit ROWS ONLY
                        """
                , params, long.class);
    }

    List<MoteNotifikasjon> hentMoterUtenVarsel(Duration fra, Duration til, int max) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("fra", ZonedDateTime.now().plus(fra))
                .addValue("til", ZonedDateTime.now().plus(til))
                .addValue("limit", max);

        return jdbc.query("""
                select a.AKTIVITET_ID, a.VERSJON, a.AKTOR_ID, a.FRA_DATO, m.KANAL from AKTIVITET a
                inner join MOTE m on a.AKTIVITET_ID = m.AKTIVITET_ID and a.VERSJON = m.VERSJON
                where GJELDENDE = 1
                and FRA_DATO between :fra and :til
                and AKTIVITET_TYPE_KODE = 'MOTE'
                and not exists(select * from GJELDENDE_MOTE_SMS SMS where a.AKTIVITET_ID = SMS.AKTIVITET_ID)
                """, params, MoteSmsDAO::mapMoteNotifikasjon);
    }

    public int slettGjeldende(long aktivitetId) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktivit_id", aktivitetId);
        return jdbc.update("delete from  GJELDENDE_MOTE_SMS where AKTIVITET_ID = :aktivit_id", params);
    }

    public void updateGjeldendeSms(MoteNotifikasjon moteNotifikasjon) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktivit_id", moteNotifikasjon.aktivitetId())
                .addValue("kanal", moteNotifikasjon.kanalDTO().name())
                .addValue("moteTid", moteNotifikasjon.startTid());

        jdbc.update(
                """
                        insert into GJELDENDE_MOTE_SMS
                                (AKTIVITET_ID, MOTETID,  KANAL, BRUKERNOTIFIKASJON)
                         values (:aktivit_id,  :moteTid, :kanal, 1)
                        """
                , params
        );
    }


}
