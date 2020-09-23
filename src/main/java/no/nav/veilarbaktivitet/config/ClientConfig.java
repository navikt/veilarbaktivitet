package no.nav.veilarbaktivitet.config;

import no.nav.common.cxf.CXFClient;
import no.nav.common.cxf.StsConfig;
import no.nav.common.rest.client.RestClient;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.utils.Credentials;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.TiltakOgAktivitetV1;
import no.nav.veilarbaktivitet.client.ArenaAktivitetClient;
import no.nav.veilarbaktivitet.client.ArenaAktivitetClientImpl;
import no.nav.veilarbaktivitet.client.KvpClient;
import no.nav.veilarbaktivitet.client.KvpClientImpl;
import no.nav.veilarbaktivitet.util.DateUtils;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Date;

import static no.nav.common.utils.EnvironmentUtils.getOptionalProperty;
import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.veilarbaktivitet.config.ApplicationContext.ARENA_AKTIVITET_DATOFILTER_PROPERTY;

@Configuration
public class ClientConfig {

    @Bean
    public OkHttpClient client(SystemUserTokenProvider tokenProvider) {
        var builder = RestClient.baseClientBuilder();
        builder.addInterceptor(new SystemUserOidcTokenProviderInterceptor(tokenProvider));
        return builder.build();
    }

    private static class SystemUserOidcTokenProviderInterceptor implements Interceptor {
        private SystemUserTokenProvider systemUserTokenProvider;

        private SystemUserOidcTokenProviderInterceptor(SystemUserTokenProvider systemUserTokenProvider) {
            this.systemUserTokenProvider = systemUserTokenProvider;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            Request newReq = original.newBuilder()
                    .addHeader("Authorization", "Bearer " + systemUserTokenProvider.getSystemUserToken())
                    .method(original.method(), original.body())
                    .build();
            return chain.proceed(newReq);
        }
    }

    @Bean
    public KvpClient kvpClient(OkHttpClient client) {
        return new KvpClientImpl(client);
    }

    @Bean
    public ArenaAktivitetClient arenaAktivitetClient(EnvironmentProperties properties, Credentials credentials) {
        Date arenaAktivitetFilterDato = getOptionalProperty(ARENA_AKTIVITET_DATOFILTER_PROPERTY)
                .map(DateUtils::parseDato)
                .orElse(null);


        StsConfig stsConfig = StsConfig.builder()
                .url(properties.getCxfStsUrl())
                .username(credentials.username)
                .password(credentials.password)
                .build();

        String endpointUrl = getRequiredProperty(ApplicationContext.VIRKSOMHET_TILTAKOGAKTIVITET_V1_ENDPOINTURL_PROPERTY);

        TiltakOgAktivitetV1 tiltakOgAktivitetV1 = new CXFClient<>(TiltakOgAktivitetV1.class)
                .withOutInterceptor(new LoggingOutInterceptor())
                .configureStsForSystemUser(stsConfig)
                .address(endpointUrl)
                .build();

        return new ArenaAktivitetClientImpl(tiltakOgAktivitetV1, arenaAktivitetFilterDato);
    }

}
