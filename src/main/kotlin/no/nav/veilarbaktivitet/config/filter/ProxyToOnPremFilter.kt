package no.nav.veilarbaktivitet.config.filter

import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Profile("!test")
@Service
@RequiredArgsConstructor
@Slf4j
class ProxyToOnPremGateway(
    @Value("\${veilarbaktivitet-fss.url}")
    private val veilaraktivitetFssUrl: String,
    private val proxyToOnPremTokenProvider: ProxyToOnPremTokenProvider) {

    @Bean
    fun myRoutes(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes()
            .route { config ->
                config
                    .path("/api/*", "/internal/api/*", "/graphql")
                    .filters { filterSpec -> filterSpec.addRequestHeader("Authorization", "Bearer ${proxyToOnPremTokenProvider.getProxyToken()}") }
                    .uri(veilaraktivitetFssUrl)
            }
            .build()
    }

}