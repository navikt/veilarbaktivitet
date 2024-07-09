package no.nav.veilarbaktivitet.config.filter;

import jakarta.servlet.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.function.Supplier;

import static no.nav.common.utils.EnvironmentUtils.isProduction;

@Profile("!test")
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ProxyFilter implements Filter {

    private final String scope = String.format("api://%s-gcp.dab.veilarbaktivitet/.default", isProduction().orElse(false) ? "prod" : "dev");

    @Autowired
    private final IAuthService authService;

    @Autowired
    private final OkHttpClient proxyHttpClient;

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

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        Request request = new Request.Builder()
                .url(servletRequest.getRemoteHost())
                .build();
        try (var response = proxyHttpClient.newCall(request).execute()) {
            log.info("Proxy ok");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}


