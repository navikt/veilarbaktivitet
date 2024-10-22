package no.nav.veilarbaktivitet.motesms;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO;
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.MinSideVarselId;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.veilarbdbutil.VeilarbAktivitetSqlParameterSource;
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

    List<Long> hentMoterMedOppdatertTidEllerKanal(int max) {
        MapSqlParameterSource params = new MapSqlParameterSource("limit", max);
        return jdbc.queryForList(
                """
                        select AKTIVITET.AKTIVITET_ID from AKTIVITET
                        inner join MOTE on AKTIVITET.AKTIVITET_ID = MOTE.AKTIVITET_ID and AKTIVITET.VERSJON = MOTE.VERSJON
                        inner join GJELDENDE_MOTE_SMS SMS on AKTIVITET.AKTIVITET_ID = SMS.AKTIVITET_ID
                        where GJELDENDE = 1
                        and (SMS.MOTETID != AKTIVITET.FRA_DATO or SMS.KANAL != MOTE.KANAL)
                        limit :limit
                        """
                , params, long.class);
    }

    List<MoteNotifikasjon> hentMoterUtenVarsel(Duration fra, Duration til, int max) {
        SqlParameterSource params = new VeilarbAktivitetSqlParameterSource()
                .addValue("fra", ZonedDateTime.now().plus(fra))
                .addValue("til", ZonedDateTime.now().plus(til))
                .addValue("avslutteteAktiviteter", List.of(AktivitetStatus.AVBRUTT.name(), AktivitetStatus.FULLFORT.name()));
        return jdbc.query("""
                select a.AKTIVITET_ID as id, a.VERSJON as aktivitet_version, a.AKTOR_ID as aktor_id, a.FRA_DATO as startdato, m.KANAL as kanal 
                from AKTIVITET a
                inner join MOTE m on a.AKTIVITET_ID = m.AKTIVITET_ID and a.VERSJON = m.VERSJON
                where GJELDENDE = 1
                and FRA_DATO between :fra and :til
                and AKTIVITET_TYPE_KODE = 'MOTE'
                and HISTORISK_DATO is null
                and LIVSLOPSTATUS_KODE not in (:avslutteteAktiviteter)
                and not exists(select * from GJELDENDE_MOTE_SMS SMS where a.AKTIVITET_ID = SMS.AKTIVITET_ID)
                """, params, MoteSmsDAO::mapMoteNotifikasjon);
    }

    @SneakyThrows
    private static MoteNotifikasjon mapMoteNotifikasjon(ResultSet rs, int r) {
        int id = rs.getInt("id");
        int version = rs.getInt("aktivitet_version");
        Person.AktorId aktorId = Person.aktorId(rs.getString("aktor_id"));
        KanalDTO kanal = KanalDTO.valueOf(rs.getString("kanal"));
        ZonedDateTime startTid = Database.hentZonedDateTime(rs, "startdato");
        return new MoteNotifikasjon(id, version, aktorId, kanal, startTid);
    }

    void slettGjeldende(long aktivitetId) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktivit_id", aktivitetId);
        jdbc.update("delete from  GJELDENDE_MOTE_SMS where AKTIVITET_ID = :aktivit_id", params);
    }

    void insertGjeldendeSms(MoteNotifikasjon moteNotifikasjon, MinSideVarselId varselId) {
        var varselIdParam = varselId != null ? varselId.getValue().toString() : null;
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
