package no.nav.veilarbaktivitet.config.filter

import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import no.nav.veilarbaktivitet.oppfolging.periode.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.https
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
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
            log.info("Gateway obo")
            ServerRequest.from(request)
                .header("Authorization", "Bearer ${getToken()}")
                .build()
        }
    }

    @Bean
    fun getRoute(): RouterFunction<ServerResponse> {
        val sendToOnPrem = https(URI.create(veilaraktivitetFssUrl))
        return route()
            .before { request ->
                log.info(request.toString());
                request
            }

            .GET("/internal/api/**", sendToOnPrem)
            .POST("/internal/api/**", sendToOnPrem)
            .PUT("/internal/api/**", sendToOnPrem)
            .DELETE("/internal/api/**", sendToOnPrem)
            .GET("/api/**", sendToOnPrem)
            .POST("/api/**", sendToOnPrem)
            .PUT("/api/**", sendToOnPrem)
            .DELETE("/api/**", sendToOnPrem)
            .POST("/graphql", sendToOnPrem)
            .POST("/veilarbaktivitet/graphql", sendToOnPrem)
//            .route(
//                path("/veilarbaktivitet/internal/isAlive")
//                    .or(
//                        path("/veilarbaktivitet/internal/isReady")
//                            .or(path("/veilarbaktivitet/internal/selftest"))
//                            .negate()
//                    ), sendToOnPrem
//            )
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