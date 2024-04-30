package no.nav.veilarbaktivitet.config

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.veilarbaktivitet.aktivitet.feil.EndringAvFerdigAktivitetException
import no.nav.veilarbaktivitet.aktivitet.feil.EndringAvHistoriskAktivitetException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler


@ControllerAdvice
class HttpExceptionHandler: ResponseEntityExceptionHandler() {

    @ExceptionHandler(value = [EndringAvFerdigAktivitetException::class, EndringAvHistoriskAktivitetException::class])
    fun handleConflict(ex: RuntimeException, request: WebRequest): ResponseEntity<Response> {
        val statusKode = 400
        return ResponseEntity
            .status(statusKode)
            .body(Response(status = statusKode, message = ex.message))
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Response(
        val message: String?,
        val status: Int,
    )
}