package no.nav.fo.veilarbaktivitet;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProviderConfig;
import no.nav.dialogarena.aktor.AktorConfig;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig;
import org.springframework.context.annotation.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

import static no.nav.fo.veilarbaktivitet.db.DatabaseContext.migrateDatabase;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.setProperty;

@Configuration
@EnableTransactionManagement
@EnableAspectJAutoProxy
@ComponentScan("no.nav.fo.veilarbaktivitet")
@Import(AktorConfig.class)
public class ApplicationContext implements ApiApplication {

    public static final String ARENA_AKTIVITET_DATOFILTER_PROPERTY = "ARENA_AKTIVITET_DATOFILTER";
    public static final String VEILARBOPPFOLGINGAPI_URL_PROPERTY = "VEILARBOPPFOLGINGAPI_URL";
    public static final String VIRKSOMHET_TILTAKOGAKTIVITET_V1_ENDPOINTURL_PROPERTY = "VIRKSOMHET_TILTAKOGAKTIVITET_V1_ENDPOINTURL";
    public static final String AKTIVITETER_FEED_BRUKERTILGANG_PROPERTY = "aktiviteter.feed.brukertilgang";
    public static final String VEILARB_KASSERING_IDENTER_PROPERTY = "VEILARB_KASSERING_IDENTER";

    public static final String AKTOER_V2_ENDPOINTURL = "AKTOER_V2_ENDPOINTURL";
    public static final String REDIRECT_URL_PROPERTY = "VEILARBLOGIN_REDIRECT_URL_URL";
    public static final String SECURITYTOKENSERVICE_URL = "SECURITYTOKENSERVICE_URL";
    public static final String ABAC_PDP_ENDPOINT_URL = "ABAC_PDP_ENDPOINT_URL";
    public static final String AKTOERREGISTER_API_V1_URL = "AKTOERREGISTER_API_V1_URL";


    @Inject
    private DataSource dataSource;

    @Override
    public void startup(ServletContext servletContext) {
        setProperty(AKTIVITETER_FEED_BRUKERTILGANG_PROPERTY, "srvveilarbportefolje", PUBLIC);
        migrateDatabase(dataSource);
    }

    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {
        apiAppConfigurator
                .sts()
                .azureADB2CLogin()
                .issoLogin()
        ;
    }

    @Bean
    public UnleashService unleashService(){
        return new UnleashService(UnleashServiceConfig.resolveFromEnvironment());
    }

    @Bean
    public SystemUserTokenProvider systemUserTokenProvider(){
        return new SystemUserTokenProvider(SystemUserTokenProviderConfig.resolveFromSystemProperties());
    }

}
