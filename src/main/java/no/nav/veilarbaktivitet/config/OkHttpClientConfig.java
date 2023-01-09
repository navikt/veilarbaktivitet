package no.nav.veilarbaktivitet.config;

import io.micrometer.core.instrument.MeterRegistry;
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
    @Bean OkHttpClient veilarboppfolgingHttpClient(MeterRegistry meterRegistry, AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient) {
        return RestClient.baseClientBuilder()
            .addInterceptor(azureM2MInterceptor(veilarboppfolgingScope, azureAdMachineToMachineTokenClient))
            .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
            .build();
    }

    @Bean OkHttpClient veilarbpersonHttpClient(MeterRegistry meterRegistry, AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient) {
        return RestClient.baseClientBuilder()
            .addInterceptor(azureM2MInterceptor(veilarbpersonScope, azureAdMachineToMachineTokenClient))
            .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
            .build();
    }

    @Bean OkHttpClient veilarbarenaHttpClient(MeterRegistry meterRegistry, AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient) {
        return RestClient.baseClientBuilder()
            .addInterceptor(azureM2MInterceptor(veilarbarenaScope, azureAdMachineToMachineTokenClient))
            .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
            .build();
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
