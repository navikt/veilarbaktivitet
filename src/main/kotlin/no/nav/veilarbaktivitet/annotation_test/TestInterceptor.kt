package no.nav.veilarbaktivitet.annotation_test

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class TestInterceptor() : HandlerInterceptor {
    private val log = LoggerFactory.getLogger(javaClass)

    @Throws(Exception::class)
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        if (handler is HandlerMethod) {
            try {
                val query = request
                    .getParameter("id")
                val pathParam = (request
                    .getAttribute("org.springframework.web.servlet.HandlerMapping.uriTemplateVariables") as Map<String, String>)["id"]
                handler.method
                log.info("Hei")
            } catch (e: Exception) {
                throw e
            }
        }
        return true
    }

}