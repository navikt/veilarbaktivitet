package no.nav.veilarbaktivitet.config.filter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import lombok.RequiredArgsConstructor;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.token_client.client.MachineToMachineTokenClient;
import no.nav.common.token_client.client.TokenXOnBehalfOfTokenClient;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.poao.dab.spring_auth.IAuthService;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.function.Supplier;

import static no.nav.common.utils.EnvironmentUtils.isProduction;

@Configuration
@RequiredArgsConstructor
public class ProxtFilterOkHttpConfig {
    private final String scope = String.format("api://%s-gcp.dab.veilarbaktivitet/.default", isProduction().orElse(false) ? "prod" : "dev");

    @Autowired
    private final IAuthService authService;

    private String getHost() {
        if (EnvironmentUtils.isProduction().orElse(false)) {
            return "https://veilarbaktivitet.prod-fss-pub.nais.io";
        } else if (EnvironmentUtils.isDevelopment().orElse(false)) {
            return "https://veilarbaktivitet.dev-fss-pub.nais.io";
        } else {
            return "http://localhost";
        }
    }

    @Bean
    protected Interceptor tokenInterceptor(AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient, MachineToMachineTokenClient machineToMachineTokenClient, TokenXOnBehalfOfTokenClient tokenXOnBehalfOfTokenClient) {
        final Supplier<String> azureAdTokenSupplier = () -> azureAdOnBehalfOfTokenClient.exchangeOnBehalfOfToken(scope, authService.getInnloggetBrukerToken());
        final Supplier<String> tokenXSupplier = () -> tokenXOnBehalfOfTokenClient.exchangeOnBehalfOfToken(scope, authService.getInnloggetBrukerToken());
        final Supplier<String> machineToMachineTokenSupplier = () -> machineToMachineTokenClient.createMachineToMachineToken(scope);

        Supplier<String> tokenSupplier = () -> {
            if(authService.erInternBruker()) {
                return azureAdTokenSupplier.get();
            } else if (authService.erEksternBruker()) {
                return tokenXSupplier.get();
            } else if (authService.erSystemBruker()) {
                return machineToMachineTokenSupplier.get();
            } else {
                throw new RuntimeException("Fant ikke supplier for proxykall. Dette burde ikke skje");
            }
        };

        return chain -> {
            Request original = chain.request();
            Request newReq = null;
            try {
                newReq = original.newBuilder()
                        .addHeader("Authorization", "Bearer " + tokenSupplier.get())
                        .method(original.method(), original.body())
                        .build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return chain.proceed(newReq);
        };
    }

    @Bean
    OkHttpClient proxyHttpClient(MeterRegistry meterRegistry, Interceptor tokenInterceptor) {

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(getHost(), 80));
        OkHttpClient client = new OkHttpClient.Builder()
                .proxy(proxy)
                .addInterceptor(tokenInterceptor)
                .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
                .build();
        return client;
    }

}
