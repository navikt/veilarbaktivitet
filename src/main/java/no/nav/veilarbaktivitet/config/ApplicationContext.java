package no.nav.veilarbaktivitet.config;

import no.nav.common.abac.Pep;
import no.nav.common.abac.VeilarbPep;
import no.nav.common.abac.audit.SpringAuditRequestInfoSupplier;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.client.aktorregister.AktorregisterHttpClient;
import no.nav.common.client.aktorregister.CachedAktorregisterClient;
import no.nav.common.cxf.CXFClient;
import no.nav.common.cxf.StsConfig;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.common.leaderelection.LeaderElectionHttpClient;
import no.nav.common.metrics.InfluxClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.sts.NaisSystemUserTokenProvider;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.utils.Credentials;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.TiltakOgAktivitetV1;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.common.utils.NaisUtils.getCredentials;

@Configuration
@EnableConfigurationProperties({EnvironmentProperties.class})
public class ApplicationContext {
    public static final String APPLICATION_NAME = "veilarbaktivitet";

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


    @Bean
    public Credentials serviceUserCredentials() {
        return getCredentials("service_user");
    }

    @Bean
    public SystemUserTokenProvider systemUserTokenProvider(EnvironmentProperties properties, Credentials serviceUserCredentials) {
        return new NaisSystemUserTokenProvider(properties.getNaisStsDiscoveryUrl(), serviceUserCredentials.username, serviceUserCredentials.password);
    }

    @Bean
    public AktorregisterClient aktorregisterClient(EnvironmentProperties properties, SystemUserTokenProvider tokenProvider) {
        AktorregisterClient aktorregisterClient = new AktorregisterHttpClient(
                properties.getAktorregisterUrl(), APPLICATION_NAME, tokenProvider::getSystemUserToken
        );
        return new CachedAktorregisterClient(aktorregisterClient);
    }

    @Bean
    public LeaderElectionClient leaderElectionClient() {
        return new LeaderElectionHttpClient();
    }

    @Bean
    public MetricsClient metricsClient() {
        return new InfluxClient();
    }

    @Bean
    public Pep veilarbPep(EnvironmentProperties properties, Credentials serviceUserCredentials) {
        return new VeilarbPep(
                properties.getAbacUrl(), serviceUserCredentials.username,
                serviceUserCredentials.password, new SpringAuditRequestInfoSupplier()
        );
    }

    @Bean
    public static TiltakOgAktivitetV1 tiltakOgAktivitetV1Client(EnvironmentProperties properties, Credentials credentials) {
        StsConfig stsConfig = StsConfig.builder()
                .url(properties.getCxfStsUrl())
                .username(credentials.username)
                .password(credentials.password)
                .build();

        return new CXFClient<>(TiltakOgAktivitetV1.class)
                .address(getRequiredProperty(ApplicationContext.VIRKSOMHET_TILTAKOGAKTIVITET_V1_ENDPOINTURL_PROPERTY))
                .withOutInterceptor(new LoggingOutInterceptor())
                .configureStsForSubject(stsConfig)
                .build();

    }
}
