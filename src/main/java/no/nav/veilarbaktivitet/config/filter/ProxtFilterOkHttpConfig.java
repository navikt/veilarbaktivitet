package no.nav.veilarbaktivitet.config.filter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import no.nav.common.utils.EnvironmentUtils;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.net.Proxy;

@Configuration
public class ProxtFilterOkHttpConfig {

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
