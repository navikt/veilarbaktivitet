package no.nav.veilarbaktivitet;

import no.nav.veilarbaktivitet.config.ApplicationTestConfig;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.db.testdriver.TestDriver;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.testutils.InsertAktiviteter;
import org.h2.tools.DeleteDbFiles;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;


@EnableAutoConfiguration
@Import(ApplicationTestConfig.class)
public class VeilarbAktivitetTestApp {

    public static void main(String[] args) {
        //LocalH2Database.setUseInnMemmory(); //uncoment to use inmemory database
        //DeleteDbFiles.execute("~/database/", "veilarbaktivitet", false);

        //We need to initialize the driver before spring starts or Flyway will not be able to use the driver
        TestDriver.init();
        System.setProperty("AKTIVITETSPLAN_URL", "kake");

        SpringApplication application = new SpringApplication(VeilarbAktivitetTestApp.class);
        application.setAdditionalProfiles("local");
        application.run(args);

        //uncoment for og lage aktiviteter /slette all data
        //JdbcTemplate jdbcTemplate = LocalH2Database.getPresistentDb();
        //InsertAktiviteter.insertAktiviteter(jdbcTemplate);
        //DbTestUtils.cleanupTestDb(jdbcTemplate);
    }
}
