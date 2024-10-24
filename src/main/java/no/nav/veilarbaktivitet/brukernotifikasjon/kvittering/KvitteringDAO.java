package no.nav.veilarbaktivitet.brukernotifikasjon.kvittering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAktivitetIder;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselStatus;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.MinSideVarselId;
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.EksternVarselHendelseDTO;
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.Kvittering;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.springframework.util.Assert.isTrue;

@Repository
@RequiredArgsConstructor
@Slf4j
public class KvitteringDAO {
    private final NamedParameterJdbcTemplate jdbc;

    RowMapper<BrukernotifikasjonAktivitetIder> rowmapper = (rs, rowNum) ->
            BrukernotifikasjonAktivitetIder.builder()
                    .id(rs.getLong("ID"))
                    .aktivitetId(rs.getLong("AKTIVITET_ID"))
                    .build();

    public void setFeilet(MinSideVarselId varselId) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("brukernotifikasjonId", varselId.getValue().toString())
                .addValue("varselKvitteringStatus", VarselKvitteringStatus.FEILET.toString());
        jdbc.update("" +
                " update BRUKERNOTIFIKASJON " +
                " set VARSEL_FEILET = current_timestamp, VARSEL_KVITTERING_STATUS = :varselKvitteringStatus " +
                " where BRUKERNOTIFIKASJON_ID = :brukernotifikasjonId ", param);
    }

    public void setEksternVarselStatusOK(MinSideVarselId varselId) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("brukernotifikasjonId", varselId.getValue().toString())
                .addValue("varselKvitteringStatus", VarselKvitteringStatus.OK.toString());

        jdbc.update("" +
                        " update BRUKERNOTIFIKASJON " +
                        " set" +
                        " BEKREFTET_SENDT = CURRENT_TIMESTAMP, " +
                        " VARSEL_KVITTERING_STATUS = :varselKvitteringStatus" +
//                        " where BRUKERNOTIFIKASJON.VARSEL_KVITTERING_STATUS != 'FEILET' " +
                        " where STATUS != 'AVSLUTTET'" +
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

        isTrue(update == 1, "Forventet en rad oppdatert, id=" + id);
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

    public void lagreKvitering(String bestillingsId, Kvittering melding, EksternVarselHendelseDTO heleMeldingen) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("BRUKERNOTIFIKASJON_ID", bestillingsId)
                .addValue("STATUS", melding.getStatus())
                .addValue("MELDING", melding.getMelding())
                .addValue("distribusjonId", null)
                .addValue("BESKJED", heleMeldingen.toString());
        jdbc.update("""
                insert into  BRUKERNOTIFIKAJSON_KVITERING_TABELL
                        (  BRUKERNOTIFIKASJON_ID,  STATUS,  MELDING,  distribusjonId,  BESKJED )
                VALUES  ( :BRUKERNOTIFIKASJON_ID, :STATUS, :MELDING, :distribusjonId, :BESKJED )
                """, parameterSource);
    }

    public void setAvsluttetHvisVarselKvitteringStatusErIkkeSatt(String brukernotifikasjonsId) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("brukernotifikasjonsId", brukernotifikasjonsId)
                .addValue("avsluttetStatus", VarselStatus.AVSLUTTET.name())
                .addValue("eksternKvitteringIkkeSattStatus", VarselKvitteringStatus.IKKE_SATT.name());
        int update = jdbc.update(
            """
                 update BRUKERNOTIFIKASJON
                 set STATUS = :avsluttetStatus
                 where BRUKERNOTIFIKASJON_ID = :brukernotifikasjonsId
                 AND VARSEL_KVITTERING_STATUS = :eksternKvitteringIkkeSattStatus
                """
                , param);

        if (update > 0) {
            log.warn("Brukernotifikasjon id={} avsluttet uten at eksterne varsel er sendt, fordi done ble sannsynligvis trigger av eksternt system.", brukernotifikasjonsId);
        }
    }
}
