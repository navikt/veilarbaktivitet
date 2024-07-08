package no.nav.veilarbaktivitet.config.filter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import jakarta.servlet.*;
import lombok.RequiredArgsConstructor;
import no.nav.common.rest.client.RestClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.token_client.client.MachineToMachineTokenClient;
import no.nav.common.token_client.client.TokenXOnBehalfOfTokenClient;
import no.nav.poao.dab.spring_auth.IAuthService;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.function.Supplier;

import static no.nav.common.utils.EnvironmentUtils.isProduction;

@Profile("!test")
@Configuration
@RequiredArgsConstructor
public class ProxyFilter implements Filter {

    private final String scope = String.format("api://%s-gcp.dab.veilarbaktivitet/.default", isProduction().orElse(false) ? "prod" : "dev");

    @Autowired
    private final IAuthService authService;

    @Autowired
    private final AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient;

    @Autowired
    private final MachineToMachineTokenClient machineToMachineTokenClient;

    @Autowired
    private final TokenXOnBehalfOfTokenClient tokenXOnBehalfOfTokenClient;

    private final Supplier<String> azureAdTokenSupplier = () -> azureAdOnBehalfOfTokenClient.exchangeOnBehalfOfToken(scope, authService.getInnloggetBrukerToken());

    private final Supplier<String> tokenXSupplier = () -> tokenXOnBehalfOfTokenClient.exchangeOnBehalfOfToken(scope, authService.getInnloggetBrukerToken());

    private final Supplier<String> machineToMachineTokenSupplier = () -> machineToMachineTokenClient.createMachineToMachineToken(scope);


    @Bean
    OkHttpClient proxyHttpClient(MeterRegistry meterRegistry, AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient) {
        return RestClient.baseClientBuilder()
                .addInterceptor(azureAdInterceptor(() ->
                        azureAdOnBehalfOfTokenClient.exchangeOnBehalfOfToken(dialogScope, authService.getInnloggetBrukerToken())
                )).eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
                .build();
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        return RestClient.baseClientBuilder()
                .addInterceptor(tokenInterceptor())
                .build();
        // veksle token
        // sende request til veilarbaktivitet i gcp
        // returnere svaret til clienten
    }

    @Bean
    protected Interceptor tokenInterceptor() {
        return chain -> {
            Request original = chain.request();
            Request newReq = null;
            try {
                newReq = original.newBuilder()
                        .addHeader("Authorization", "Bearer " + getToken())
                        .method(original.method(), original.body())
                        .build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return chain.proceed(newReq);
        };
    }

    private String getToken() throws Exception {
        if(authService.erInternBruker()) {
            return azureAdTokenSupplier.get();
        } else if (authService.erEksternBruker()) {
            return tokenXSupplier.get();
        } else if (authService.erSystemBruker()) {
            return machineToMachineTokenSupplier.get();
        } else {
            throw new Exception("Fant ikke supplier for proxykall. Dette burde ikke skje");
        }
    }
//    @Bean
//    OkHttpClient proxyHttpClient(MeterRegistry meterRegistry, AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient) {
//        return RestClient.baseClientBuilder()
//                .addInterceptor(azureAdInterceptor(() ->
//                        azureAdOnBehalfOfTokenClient.exchangeOnBehalfOfToken(dialogScope, authService.getInnloggetBrukerToken())
//                )).eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
//                .build();
//    }
}


