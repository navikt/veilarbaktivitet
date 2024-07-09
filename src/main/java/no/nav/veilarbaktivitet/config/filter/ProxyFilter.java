package no.nav.veilarbaktivitet.config.filter;

import jakarta.servlet.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

@Profile("!test")
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ProxyFilter implements Filter {
    @Autowired
    private final OkHttpClient proxyHttpClient;

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


