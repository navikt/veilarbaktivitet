package no.nav.fo.veilarbaktivitet.kafka;

import io.micrometer.core.instrument.Gauge;
import lombok.SneakyThrows;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData;
import no.nav.sbl.sql.SQLFunction;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.List;

import static no.nav.metrics.MetricsFactory.getMeterRegistry;

@Component
public class KafkaDAO {

    private JdbcTemplate jdbcTemplate;
    public static String TABLE_NAME = "FEILEDE_KAFKA_MELDINGER";

    @Inject
    public KafkaDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        Gauge.builder("kafka_feilede_meldinger_aktivitet_oppdatert", this::antallFeiledeMeldinger).register(getMeterRegistry());
    }

    @SneakyThrows
    public void lagre(KafkaAktivitetMelding melding) {
        SqlUtils.insert(jdbcTemplate, TABLE_NAME)
                .value("MELDING_ID", melding.getNavCallId())
                .value("AKTIVITET_ID", melding.getAktivitetId())
                .value("AKTOR_ID", melding.getAktorId())
                .value("FRA_DATO", melding.getFraDato())
                .value("TIL_DATO", melding.getFraDato())
                .value("ENDRET_DATO", melding.getEndretDato())
                .value("AKTIVITET_TYPE", melding.getAktivitetType().name())
                .value("AKTIVITET_STATUS", melding.getAktivitetStatus().name())
                .value("AVTALT", melding.getAvtalt())
                .value("HISTORISK", melding.getHistorisk())
                .execute();
    }

    public void slett(KafkaAktivitetMelding melding) {
        SqlUtils.delete(jdbcTemplate, TABLE_NAME)
                .where(WhereClause.equals("MELDING_ID", melding.getNavCallId()))
                .execute();
    }

    public List<KafkaAktivitetMelding> hentFeiledeMeldinger() {
        return SqlUtils
                .select(jdbcTemplate, TABLE_NAME, mapper())
                .column("*")
                .executeToList();
    }

    private SQLFunction<ResultSet, KafkaAktivitetMelding> mapper() {
        return rs -> KafkaAktivitetMelding.builder()
                .navCallId(rs.getString("MELDING_ID"))
                .aktivitetId(rs.getLong("AKTIVITET_ID"))
                .aktorId(rs.getString("AKTOR_ID"))
                .fraDato(rs.getTimestamp("FRA_DATO"))
                .tilDato(rs.getTimestamp("TIL_DATO"))
                .endretDato(rs.getTimestamp("ENDRET_DATO"))
                .aktivitetType(AktivitetTypeData.valueOf(rs.getString("AKTIVITET_TYPE")))
                .aktivitetStatus(AktivitetStatus.valueOf(rs.getString("AKTIVITET_STATUS")))
                .avtalt(rs.getBoolean("AVTALT"))
                .historisk(rs.getBoolean("HISTORISK"))
                .build();
    }

    public Long antallFeiledeMeldinger() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + TABLE_NAME, Long.class);
    }
}
