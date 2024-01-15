package no.nav.veilarbaktivitet.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import lombok.RequiredArgsConstructor;
import no.nav.common.rest.client.RestClient;
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.poao.dab.spring_auth.IAuthService;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.function.Supplier;

import static no.nav.common.utils.EnvironmentUtils.isProduction;

@Configuration
@RequiredArgsConstructor
public class OkHttpClientConfig {

    private final IAuthService authService;

    @Bean OkHttpClient veilarboppfolgingHttpClient(MeterRegistry meterRegistry, AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient) {
        return RestClient.baseClientBuilder()
                .addInterceptor(azureAdInterceptor(() -> azureAdMachineToMachineTokenClient.createMachineToMachineToken(veilarboppfolgingScope)))
            .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
            .build();
    }

    @Bean OkHttpClient veilarbpersonHttpClient(MeterRegistry meterRegistry, AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient) {
        return RestClient.baseClientBuilder()
                .addInterceptor(azureAdInterceptor(() -> azureAdMachineToMachineTokenClient.createMachineToMachineToken(veilarbpersonScope)))
            .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
            .build();
    }

    @Bean OkHttpClient veilarbarenaHttpClient(MeterRegistry meterRegistry, AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient) {
        return RestClient.baseClientBuilder()
            .addInterceptor(azureAdInterceptor(() -> azureAdMachineToMachineTokenClient.createMachineToMachineToken(veilarbarenaScope)))
            .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
            .build();
    }

    @Bean OkHttpClient orkivarHttpClient(MeterRegistry meterRegistry, AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient) {
        return RestClient.baseClientBuilder()
            .addInterceptor(azureAdInterceptor(() ->
                    azureAdOnBehalfOfTokenClient.exchangeOnBehalfOfToken(orkivarScope, authService.getInnloggetBrukerToken())
            )).eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
            .build();
    }

    private final String veilarboppfolgingScope = String.format("api://%s-fss.pto.veilarboppfolging/.default", isProduction().orElse(false) ? "prod" : "dev");
    private final String veilarbpersonScope = String.format("api://%s-fss.pto.veilarbperson/.default", isProduction().orElse(false) ? "prod" : "dev");
    private final String veilarbarenaScope = String.format("api://%s-fss.pto.veilarbarena/.default", isProduction().orElse(false) ? "prod" : "dev");
    private final String orkivarScope = String.format("api://%s-gcp.dab.orkivar/.default", isProduction().orElse(false) ? "prod" : "dev");

    @Bean
    @Profile("!dev")
    public AzureAdMachineToMachineTokenClient machineToMachineTokenClient() {
        return AzureAdTokenClientBuilder.builder()
            .withNaisDefaults()
            .buildMachineToMachineTokenClient();
    }

    @Bean
    @Profile("!dev")
    public AzureAdOnBehalfOfTokenClient onBehalfOfTokenClient() {
        return AzureAdTokenClientBuilder.builder()
                .withNaisDefaults()
                .buildOnBehalfOfTokenClient();
    }

    private Interceptor azureAdInterceptor(Supplier<String> getToken) {
        return chain -> {
            Request original = chain.request();
            Request newReq = original.newBuilder()
                    .addHeader("Authorization", "Bearer " + getToken.get())
                    .method(original.method(), original.body())
                    .build();
            return chain.proceed(newReq);
        };
    }
}
