package no.nav.veilarbaktivitet.config.filter

import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import no.nav.veilarbaktivitet.oppfolging.periode.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.https
import org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import java.net.URI


@Profile("!test")
@Service
@RequiredArgsConstructor
@Slf4j
class ProxyToOnPremGateway(
    @Value("\${veilarbaktivitet-fss.url}")
    private val veilaraktivitetFssUrl: String,
    private val proxyToOnPremTokenProvider: ProxyToOnPremTokenProvider
) {

    private fun oboExchange(getToken: () -> String): (ServerRequest) -> ServerRequest {
        return { request ->
            log.info("Gateway obo $request")
            ServerRequest.from(request)
                .headers {
                    val oldAuthHeader = it[HttpHeaders.AUTHORIZATION]
                    it.replace(HttpHeaders.AUTHORIZATION, oldAuthHeader, listOf("Bearer ${getToken()}"))
                }
                .build()
        }
    }

    @ConditionalOnProperty(name = ["spring.cloud.gateway.mvc.enabled"], havingValue = "true")
    @Order(-1)
    @Bean
    fun getRoute(): RouterFunction<ServerResponse> {
        val sendToOnPrem = https(URI.create(veilaraktivitetFssUrl))
        return route()
            .route(
                path("/internal/isAlive")
                    .or(path("/internal/isReady")
                    .or(path("/internal/selftest"))
                    .or(path("/internal/kassering"))
                    .or(path("/internal/kassering/**"))
//                    .or(path("/internal/kassering/dialog"))
//                    .or(path("/internal/kassering/dialog/"))
                ).negate(), sendToOnPrem
            )
            .before(oboExchange { proxyToOnPremTokenProvider.getProxyToken() })
            .onError({ error ->
                log.error("Proxy error", error)
                true
            }, { error, request ->
                ServerResponse
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error.stackTraceToString())
            })
            .build()
    }
}