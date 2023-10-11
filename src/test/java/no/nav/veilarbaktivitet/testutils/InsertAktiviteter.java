package no.nav.veilarbaktivitet.testutils;

import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class InsertAktiviteter {


    public static void insertAktiviteter(JdbcTemplate jdbcTemplate) {
        AktivitetDAO aktivitetDAO = new AktivitetDAO(new NamedParameterJdbcTemplate(jdbcTemplate));
        for (int i = 0; i < 10; i++) {
            insertEnAvHver(Person.aktorId(i + "auto"), aktivitetDAO);
        }
    }

    public static void insertEnAvHver(Person.AktorId akotrid, AktivitetDAO aktivitetDAO) {
        insertNyAktivitet(AktivitetDataTestBuilder.nyBehandlingAktivitet(), akotrid, aktivitetDAO);
        insertNyAktivitet(AktivitetDataTestBuilder.nyIJobbAktivitet(), akotrid, aktivitetDAO);
        insertNyAktivitet(AktivitetDataTestBuilder.nyMoteAktivitet(), akotrid, aktivitetDAO);
        insertNyAktivitet(AktivitetDataTestBuilder.nySokeAvtaleAktivitet(), akotrid, aktivitetDAO);
        insertNyAktivitet(AktivitetDataTestBuilder.nyEgenaktivitet(), akotrid, aktivitetDAO);
        insertNyAktivitet(AktivitetDataTestBuilder.nySamtaleReferat(), akotrid, aktivitetDAO);
        insertNyAktivitet(AktivitetDataTestBuilder.nyttStillingssok(), akotrid, aktivitetDAO);
    }

    private static void insertNyAktivitet(AktivitetData aktivitetData, Person.AktorId aktorId, AktivitetDAO aktivitetDAO) {
        AktivitetData build = aktivitetData.toBuilder().aktorId(aktorId).build();
        aktivitetDAO.opprettNyAktivitet(build);
    }
}
