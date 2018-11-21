package no.nav.fo.veilarbaktivitet;

import no.nav.apiapp.ApiApplication.NaisApiApplication;
import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.dialogarena.aktor.AktorConfig;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.setProperty;

@Configuration
@EnableTransactionManagement
@EnableAspectJAutoProxy
@ComponentScan("no.nav.fo.veilarbaktivitet")
@Import(AktorConfig.class)
public class ApplicationContext implements NaisApiApplication {

    public static final String APPLICATION_NAME = "veilarbaktivitet";
    public static final String ARENA_AKTIVITET_DATOFILTER_PROPERTY = "ARENA_AKTIVITET_DATOFILTER";
    public static final String AKTOER_V2_URL_PROPERTY = "AKTOER_V2_ENDPOINTURL";
    public static final String VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY = "VEILARBLOGIN_REDIRECT_URL_URL";
    public static final String VEILARBAZUREADPROXY_DISCOVERY_URL_PROPERTY = "VEILARBAZUREADPROXY_DISCOVERY_URL";
    public static final String AAD_B2C_CLIENTID_USERNAME_PROPERTY = "AAD_B2C_CLIENTID_USERNAME";
    public static final String AAD_B2C_CLIENTID_PASSWORD_PROPERTY = "AAD_B2C_CLIENTID_PASSWORD";
    public static final String VEILARBOPPFOLGINGAPI_URL_PROPERTY = "VEILARBOPPFOLGINGAPI_URL";
    public static final String VIRKSOMHET_TILTAK_OG_AKTIVITET_V1_URL_PROPERTY = "VIRKSOMHET_TILTAK_OG_AKTIVITET_V1_ENDPOINTURL";
    public static final String AKTIVITETER_FEED_BRUKERTILGANG_PROPERTY = "AKTIVITETER_FEED_BRUKERTILGANG";
    public static final String VEILARB_KASSERING_IDENTER_PROPERTY = "VEILARB_KASSERING_IDENTER";

    @Inject
    private DataSource dataSource;

    @Override
    public void startup(ServletContext servletContext) {
        setProperty(AKTIVITETER_FEED_BRUKERTILGANG_PROPERTY, "srvveilarbportefolje", PUBLIC);

        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.migrate();
    }

    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {
        apiAppConfigurator
                .sts()
                .azureADB2CLogin()
                .issoLogin()
        ;
    }
}
