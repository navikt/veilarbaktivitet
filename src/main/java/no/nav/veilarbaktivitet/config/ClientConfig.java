package no.nav.veilarbaktivitet.config;

import no.nav.common.rest.client.RestClient;
import no.nav.common.sts.SystemUserTokenProvider;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

@Profile("!dev")
@Configuration
public class ClientConfig {

    @Bean
    public OkHttpClient client(SystemUserTokenProvider tokenProvider) {
        var builder = RestClient.baseClientBuilder();
        builder.addInterceptor(new SystemUserOidcTokenProviderInterceptor(tokenProvider));
        return builder.build();
    }

    private static class SystemUserOidcTokenProviderInterceptor implements Interceptor {
        private SystemUserTokenProvider systemUserTokenProvider;

        private SystemUserOidcTokenProviderInterceptor(SystemUserTokenProvider systemUserTokenProvider) {
            this.systemUserTokenProvider = systemUserTokenProvider;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            Request newReq = original.newBuilder()
                    .addHeader("Authorization", "Bearer " + systemUserTokenProvider.getSystemUserToken())
                    .method(original.method(), original.body())
                    .build();
            return chain.proceed(newReq);
        }
    }
}
