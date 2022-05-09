package no.nav.veilarbaktivitet.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import no.nav.common.rest.client.RestClient;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class OkHttpClientConfig {

    @Bean
    public OkHttpClient client(SystemUserTokenProvider tokenProvider, MeterRegistry meterRegistry) {
        var builder = RestClient.baseClientBuilder();
        builder.addInterceptor(new SystemUserOidcTokenProviderInterceptor(tokenProvider));
        builder.eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests")
                .build());
        return builder.build();
    }

    @Bean
    public OkHttpClient veilarbarenaClient(SystemUserTokenProvider tokenProvider, MeterRegistry meterRegistry) {
        var builder = RestClient.baseClientBuilder();
        builder.addInterceptor(new SystemUserOidcTokenProviderInterceptor(tokenProvider));
        builder.eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests")
                .build());
        return builder.build();
    }

    @Bean
    public AzureAdMachineToMachineTokenClient tokenClient() {
        return AzureAdTokenClientBuilder.builder()
            .withNaisDefaults()
            .buildMachineToMachineTokenClient();
    }

    private static class SystemUserOidcTokenProviderInterceptor implements Interceptor {
        private final SystemUserTokenProvider systemUserTokenProvider;

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
}
