package no.nav.veilarbaktivitet.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import no.nav.common.rest.client.RestClient;
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class OkHttpClientConfig {

    @Bean
    public OkHttpClient veilarbarenaHttpClient() {
        var builder = RestClient.baseClientBuilder();
        return builder.build();
    }

    @Bean
    public OkHttpClient veilarboppfolgingHttpClient(MeterRegistry meterRegistry) {
        var builder = RestClient.baseClientBuilder();
        builder.eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build());
        return builder.build();
    }

    @Bean
    @Profile("!dev")
    public AzureAdMachineToMachineTokenClient tokenClient() {
        return AzureAdTokenClientBuilder.builder()
            .withNaisDefaults()
            .buildMachineToMachineTokenClient();
    }

}
