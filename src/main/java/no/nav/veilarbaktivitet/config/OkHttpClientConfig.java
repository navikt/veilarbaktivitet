package no.nav.veilarbaktivitet.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import lombok.RequiredArgsConstructor;
import no.nav.common.rest.client.RestClient;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.token_client.client.TokenXOnBehalfOfTokenClient;
import no.nav.poao.dab.spring_auth.IAuthService;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static no.nav.common.utils.EnvironmentUtils.isProduction;

@Configuration
@RequiredArgsConstructor
public class OkHttpClientConfig {

    private final IAuthService authService;

    @Bean OkHttpClient veilarboppfolgingHttpClient(MeterRegistry meterRegistry, AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient) {
        return RestClient.baseClientBuilder()
                .addInterceptor(tokenInterceptor(() -> azureAdMachineToMachineTokenClient.createMachineToMachineToken(veilarboppfolgingScope)))
            .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
            .build();
    }

    @Bean OkHttpClient veilarboppfolgingOnBehalfOfHttpClient(MeterRegistry meterRegistry, AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient, TokenXOnBehalfOfTokenClient tokenXOnBehalfOfTokenClient) {
        var tokenClient = authService.erInternBruker() ? azureAdOnBehalfOfTokenClient : tokenXOnBehalfOfTokenClient;
        return RestClient.baseClientBuilder()
                .addInterceptor(tokenInterceptor(() -> tokenClient.exchangeOnBehalfOfToken(veilarboppfolgingScope, authService.getInnloggetBrukerToken())))
                .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
                .build();
    }

    @Bean OkHttpClient veilarbarenaHttpClient(MeterRegistry meterRegistry, AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient) {
        return RestClient.baseClientBuilder()
            .addInterceptor(tokenInterceptor(() -> azureAdMachineToMachineTokenClient.createMachineToMachineToken(veilarbarenaScope)))
            .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
            .build();
    }

    @Bean OkHttpClient orkivarHttpClient(MeterRegistry meterRegistry, AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient, TokenXOnBehalfOfTokenClient tokenXOnBehalfOfTokenClient) {
        var tokenClient = authService.erInternBruker() ? azureAdOnBehalfOfTokenClient : tokenXOnBehalfOfTokenClient;
        return RestClient.baseClientBuilder()
            .connectTimeout(15, TimeUnit.SECONDS) // Dokark and pdf-gen is very slow
            .writeTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(tokenInterceptor(() ->
                    tokenClient.exchangeOnBehalfOfToken(orkivarScope, authService.getInnloggetBrukerToken())
            )).eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
            .build();
    }

    @Bean OkHttpClient dialogHttpClient(MeterRegistry meterRegistry, AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient, TokenXOnBehalfOfTokenClient tokenXOnBehalfOfTokenClient) {
        var tokenClient = authService.erInternBruker() ? azureAdOnBehalfOfTokenClient : tokenXOnBehalfOfTokenClient;
        return RestClient.baseClientBuilder()
                .addInterceptor(tokenInterceptor(() ->
                        tokenClient.exchangeOnBehalfOfToken(dialogScope, authService.getInnloggetBrukerToken())
                )).eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
                .build();
    }

    private final String veilarboppfolgingScope = "api://%s-gcp.poao.veilarboppfolging/.default".formatted(isProduction().orElse(false) ? "prod" : "dev");
    private final String veilarbarenaScope = "api://%s-fss.pto.veilarbarena/.default".formatted(isProduction().orElse(false) ? "prod" : "dev");
    private final String orkivarScope = "api://%s-gcp.dab.orkivar/.default".formatted(isProduction().orElse(false) ? "prod" : "dev");
    private final String dialogScope = "api://%s-gcp.dab.veilarbdialog/.default".formatted(isProduction().orElse(false) ? "prod" : "dev");

    private Interceptor tokenInterceptor(Supplier<String> getToken) {
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
