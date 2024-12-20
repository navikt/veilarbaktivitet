package no.nav.veilarbaktivitet.config

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.veilarbaktivitet.aktivitet.feil.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler


@ControllerAdvice
class HttpExceptionHandler : ResponseEntityExceptionHandler() {
    val log = LoggerFactory.getLogger(HttpExceptionHandler::class.java)

    @ExceptionHandler(value = [EndringAvAktivitetException::class])
    fun handleException(e: EndringAvAktivitetException, request: WebRequest): ResponseEntity<Response> {
        val statusKode = when (e) {
            is EndringAvFerdigAktivitetException, is EndringAvHistoriskAktivitetException -> HttpStatus.BAD_REQUEST.value()
            is EndringAvUtdatertVersjonException, is AktivitetVersjonOutOfOrderException -> HttpStatus.CONFLICT.value()
        }
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