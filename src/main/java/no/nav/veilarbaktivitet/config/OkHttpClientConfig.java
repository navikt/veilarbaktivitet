package no.nav.veilarbaktivitet.config;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import no.nav.common.rest.client.RestClient;
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import okhttp3.EventListener;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static no.nav.common.utils.EnvironmentUtils.isProduction;

@Configuration
public class OkHttpClientConfig {
    @Bean OkHttpClient veilarboppfolgingHttpClient(EventListener metricListener, AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient) {
        return RestClient.baseClientBuilder()
            .addInterceptor(azureM2MInterceptor(veilarboppfolgingScope, azureAdMachineToMachineTokenClient))
            .eventListener(metricListener)
            .build();
    }

    @Bean OkHttpClient veilarbpersonHttpClient(EventListener metricListener, AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient) {
        return RestClient.baseClientBuilder()
            .addInterceptor(azureM2MInterceptor(veilarbpersonScope, azureAdMachineToMachineTokenClient))
            .eventListener(metricListener)
            .build();
    }

    @Bean OkHttpClient veilarbarenaHttpClient(EventListener metricListener, AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient) {
        return RestClient.baseClientBuilder()
            .addInterceptor(azureM2MInterceptor(veilarbarenaScope, azureAdMachineToMachineTokenClient))
            .eventListener(metricListener)
            .build();
    }

    @Bean
    @Profile("!dev")
    EventListener metricListener(MeterRegistry meterRegistry) {
        return OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build();
    }

    private final String veilarboppfolgingScope = String.format("api://%s-fss.pto.veilarboppfolging/.default", isProduction().orElse(false) ? "prod" : "dev");
    private final String veilarbpersonScope = String.format("api://%s-fss.pto.veilarbperson/.default", isProduction().orElse(false) ? "prod" : "dev");
    private final String veilarbarenaScope = String.format("api://%s-fss.pto.veilarbarena/.default", isProduction().orElse(false) ? "prod" : "dev");

    @Bean
    @Profile("!dev")
    public AzureAdMachineToMachineTokenClient tokenClient() {
        return AzureAdTokenClientBuilder.builder()
            .withNaisDefaults()
            .buildMachineToMachineTokenClient();
    }
    private Interceptor azureM2MInterceptor(String scope, AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient) {
        return chain -> {
            Request original = chain.request();
            Request newReq = original.newBuilder()
                    .addHeader("Authorization", "Bearer " + azureAdMachineToMachineTokenClient.createMachineToMachineToken(scope))
                    .method(original.method(), original.body())
                    .build();
            return chain.proceed(newReq);
        };
    }
}
