package no.nav.veilarbaktivitet.config

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.veilarbaktivitet.aktivitet.feil.EndringAvAktivitetException
import no.nav.veilarbaktivitet.aktivitet.feil.EndringAvFerdigAktivitetException
import no.nav.veilarbaktivitet.aktivitet.feil.EndringAvHistoriskAktivitetException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler


@ControllerAdvice
class HttpExceptionHandler: ResponseEntityExceptionHandler() {

    @ExceptionHandler(value = [EndringAvFerdigAktivitetException::class, EndringAvHistoriskAktivitetException::class])
    fun handleException(e: EndringAvAktivitetException, request: WebRequest): ResponseEntity<Response> {
        val statusKode = 400
        return ResponseEntity
            .status(statusKode)
            .body(Response(statusCode = statusKode, message = e.message))
    }

    @ExceptionHandler(value = [ResponseStatusException::class])
    fun handleResponseStatusException(e: ResponseStatusException, request: WebRequest): ResponseEntity<Response> {
        return ResponseEntity
            .status(e.statusCode)
            .body(Response(statusCode = e.statusCode.value(), message = e.reason))
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Response(
        val message: String?,
        val statusCode: Int,
    )
}