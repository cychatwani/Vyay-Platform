package com.vyay.core.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.vyay.core.dto.wrapper.ApiResponse;
import com.vyay.core.exception.auth.AuthException;
import com.vyay.core.exception.business.BusinessException;
import com.vyay.core.exception.business.StaleVersionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.http.HttpMethod;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ValidationErrorProcessor validationErrorProcessor;

    public GlobalExceptionHandler(ValidationErrorProcessor validationErrorProcessor) {
        this.validationErrorProcessor = validationErrorProcessor;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        ApiResponse<Object> response = validationErrorProcessor.processGlobalValidation(ex);
        if (response != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Validation error");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(details, "ERR_VALIDATION"));
    }

    /**
     * A required @RequestParam was not sent at all — e.g. GET .../settlement-plan
     * with no currencyCode.
     * <p>
     * Handled explicitly because Spring raises this BEFORE the controller method is
     * invoked, so without a handler here it falls through to {@link #handleGeneral}
     * and the caller gets 500 / UNKNOWN_ERROR for what is plainly their own
     * malformed request — while the server logs a stack trace at ERROR for it.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<String>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        "Required parameter '" + ex.getParameterName() + "' is missing.",
                        "ERR_MISSING_PARAMETER"));
    }

    /**
     * A @RequestParam or @PathVariable was sent but could not be converted to the
     * declared type — a malformed UUID in the path, a non-numeric page, an unknown
     * enum constant. Same reason as above: raised during argument binding, so it
     * would otherwise surface as a 500.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Class<?> required = ex.getRequiredType();
        String message = (required != null && required.isEnum())
                ? "Invalid value '" + ex.getValue() + "' for '" + ex.getName() + "'. Allowed values: "
                        + Arrays.stream(required.getEnumConstants())
                                .map(Object::toString)
                                .collect(Collectors.joining(", "))
                : "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'.";

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message, "ERR_INVALID_PARAMETER"));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<String>> handleAuthException(AuthException ex) {
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<String>> handleBusinessException(BusinessException ex) {
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<String>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage(), "ERR_BAD_CREDENTIALS"));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleNotFound(NoHandlerFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("The requested endpoint was not found", "ERR_NOT_FOUND"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String used = ex.getMethod();
        Set<HttpMethod> supportedSet = ex.getSupportedHttpMethods();
        String supported = (supportedSet == null || supportedSet.isEmpty())
                ? "N/A"
                : supportedSet.stream().map(HttpMethod::name).collect(Collectors.joining(", "));

        String msg = "HTTP method " + used + " is not allowed for this endpoint. Supported: " + supported;
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(msg, "ERR_METHOD_NOT_ALLOWED"));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<String>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        // Translate JPA's optimistic-lock failure into our typed BusinessException shape.
        // Logged at info — this is an expected client-recoverable conflict, not a server fault.
        log.info("Optimistic lock conflict on {} (id={})", ex.getPersistentClassName(), ex.getIdentifier());
        StaleVersionException sve = new StaleVersionException();
        return ResponseEntity
                .status(sve.getHttpStatus())
                .body(ApiResponse.error(sve.getMessage(), sve.getErrorCode()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Sorry, something went wrong", "UNKNOWN_ERROR"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<String>> handleUnreadable(HttpMessageNotReadableException ex) {
        String message = "Malformed or unreadable request body";
        if (ex.getCause() instanceof InvalidFormatException ife
                && ife.getTargetType() != null && ife.getTargetType().isEnum()) {
            String allowed = Arrays.stream(ife.getTargetType().getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            message = "Invalid value '" + ife.getValue() + "'. Allowed values: " + allowed;
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message, "ERR_MALFORMED_REQUEST"));
    }
}
