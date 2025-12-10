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

    @Bean
    OkHttpClient veilarboppfolgingHttpClient(MeterRegistry meterRegistry, AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient) {
        return RestClient.baseClientBuilder()
                .addInterceptor(tokenInterceptor(() -> azureAdMachineToMachineTokenClient.createMachineToMachineToken(getVeilarboppfolgingScope(true))))
                .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
                .build();
    }

    @Bean
    OkHttpClient veilarboppfolgingOnBehalfOfHttpClient(MeterRegistry meterRegistry, AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient, TokenXOnBehalfOfTokenClient tokenXOnBehalfOfTokenClient) {
        return RestClient.baseClientBuilder()
                .addInterceptor(tokenInterceptor(() -> {
                    var tokenClient = authService.erInternBruker() ? azureAdOnBehalfOfTokenClient : tokenXOnBehalfOfTokenClient;
                    return tokenClient.exchangeOnBehalfOfToken(getVeilarboppfolgingScope(authService.erInternBruker()), authService.getInnloggetBrukerToken());
                }))
                .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
                .build();
    }

    @Bean
    OkHttpClient veilarbarenaHttpClient(MeterRegistry meterRegistry, AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient) {
        return RestClient.baseClientBuilder()
                .addInterceptor(tokenInterceptor(() -> azureAdMachineToMachineTokenClient.createMachineToMachineToken(getVeilarbarenaScope(true))))
                .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
                .build();
    }

    @Bean
    OkHttpClient orkivarHttpClient(MeterRegistry meterRegistry, AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient, TokenXOnBehalfOfTokenClient tokenXOnBehalfOfTokenClient) {
        return RestClient.baseClientBuilder()
                .connectTimeout(15, TimeUnit.SECONDS) // Dokark and pdf-gen is very slow
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(tokenInterceptor(() -> {
                    var tokenClient = authService.erInternBruker() ? azureAdOnBehalfOfTokenClient : tokenXOnBehalfOfTokenClient;
                    return tokenClient.exchangeOnBehalfOfToken(getOrkivarScope(authService.erInternBruker()), authService.getInnloggetBrukerToken());
                }))
                .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
                .build();
    }

    @Bean
    OkHttpClient dialogHttpClient(MeterRegistry meterRegistry, AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient, TokenXOnBehalfOfTokenClient tokenXOnBehalfOfTokenClient) {
        return RestClient.baseClientBuilder()
                .addInterceptor(tokenInterceptor(() -> {
                    var tokenClient = authService.erInternBruker() ? azureAdOnBehalfOfTokenClient : tokenXOnBehalfOfTokenClient;
                    return tokenClient.exchangeOnBehalfOfToken(getDialogScope(authService.erInternBruker()), authService.getInnloggetBrukerToken());
                }))
                .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
                .build();
    }

    private String getVeilarboppfolgingScope(Boolean forAzureToken) {
        String env = isProduction().orElse(false) ? "prod" : "dev";
        if (forAzureToken) {
            return "api://%s-gcp.poao.veilarboppfolging/.default".formatted(env);
        } else {
            return "%s-gcp:poao:veilarboppfolging".formatted(env);
        }
    }

    private String getVeilarbarenaScope(Boolean forAzureToken) {
        String env = isProduction().orElse(false) ? "prod" : "dev";
        if (forAzureToken) {
            return "api://%s-fss.pto.veilarbarena/.default".formatted(env);
        } else {
            return "%s-gcp:dab:veilarbarena".formatted(env);
        }
    }

    private String getOrkivarScope(Boolean forAzureToken) {
        String env = isProduction().orElse(false) ? "prod" : "dev";
        if (forAzureToken) {
            return "api://%s-gcp.dab.orkivar/.default".formatted(env);
        } else {
            return "%s-gcp:dab:orkivar".formatted(env);
        }
    }

    private String getDialogScope(Boolean forAzureToken) {
        String env = isProduction().orElse(false) ? "prod" : "dev";
        if (forAzureToken) {
            return "api://%s-gcp.dab.veilarbdialog/.default".formatted(env);
        } else {
            return "%s-gcp:dab:veilarbdialog".formatted(env);
        }
    }

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
