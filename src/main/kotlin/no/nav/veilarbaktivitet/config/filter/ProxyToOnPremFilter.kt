package no.nav.veilarbaktivitet.config.filter

import com.netflix.zuul.ZuulFilter
import com.netflix.zuul.context.RequestContext
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.ROUTE_TYPE
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Profile("!test")
@Service
@RequiredArgsConstructor
@Slf4j
class ProxyToOnPremFilter(private val proxyToOnPremTokenProvider: ProxyToOnPremTokenProvider): ZuulFilter() {

    override fun shouldFilter(): Boolean {
        return true
    }

    override fun run() {
        val token = proxyToOnPremTokenProvider.getProxyToken()
        val context = RequestContext.getCurrentContext()
        context.addZuulRequestHeader("Authorization", "Bearer $token")
    }

    override fun filterType(): String {
        return ROUTE_TYPE
    }

    override fun filterOrder(): Int {
        return 6 // Tatt hensyn til de andre filtrene som er wiret up i FilterConfig
    }
}