package com.arbitrage.cap7.rest;

import com.arbitrage.cap7.dto.Dtos.ApiError;
import com.arbitrage.cap7.service.ConflictException;
import com.arbitrage.cap7.service.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One place that turns exceptions from every REST controller into clear JSON errors
 * with the right HTTP status code, instead of a stack trace.
 */
@RestControllerAdvice(basePackages = "com.arbitrage.cap7.rest")
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException e, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, e.getMessage(), req, null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> conflict(ConflictException e, HttpServletRequest req) {
        return error(HttpStatus.CONFLICT, e.getMessage(), req, null);
    }

    /** @Valid failed: 400 with one message per field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalid(MethodArgumentNotValidException e, HttpServletRequest req) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "Some fields are not valid", req, fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> badRequest(Exception e, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "The request is malformed: " + firstLine(e.getMessage()), req, null);
    }

    /** MySQL is down or refused: 503 Service Unavailable, so clients know to try again later. */
    @ExceptionHandler({CannotCreateTransactionException.class, DataAccessException.class})
    public ResponseEntity<ApiError> database(Exception e, HttpServletRequest req) {
        log.warn("Database problem on {}: {}", req.getRequestURI(), firstLine(e.getMessage()));
        return error(HttpStatus.SERVICE_UNAVAILABLE, "The database is not available. Is MySQL running?", req, null);
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String message, HttpServletRequest req,
                                                  Map<String, String> fields) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message,
                req.getRequestURI(), fields == null ? Map.of() : fields);
        return ResponseEntity.status(status).body(body);
    }

    private static String firstLine(String text) {
        return text == null ? "" : text.lines().findFirst().orElse("");
    }
}
