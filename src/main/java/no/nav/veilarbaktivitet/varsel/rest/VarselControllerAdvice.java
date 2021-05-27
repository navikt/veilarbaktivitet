package no.nav.veilarbaktivitet.varsel.rest;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.varsel.exceptions.VarselException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
public class VarselControllerAdvice extends ResponseEntityExceptionHandler {

    private static final boolean DEBUG_REST = true;

    @ExceptionHandler(VarselException.class)
    public ResponseEntity<String> handleUnauthorizedException(VarselException e, WebRequest webRequest) {
        log.warn(e.getMessage(), e);
        return sendResponse(e, webRequest, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity sendResponse(RuntimeException e, WebRequest request, HttpStatus status) {
        if (DEBUG_REST) {
            final String returnBody =
                    "message: " + e.getMessage() + "\n\n" +
                            "stacktrace: " + ExceptionUtils.getStackTrace(e);

            return handleExceptionInternal(e, returnBody, new HttpHeaders(), status, request);
        }

        return ResponseEntity.status(status).build();
    }
}
