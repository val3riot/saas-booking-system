package it.booking.common;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        log.warn("Application error on {}: code={}, status={}, message={}",
                request.getRequestURI(),
                ex.errorCode().code(),
                ex.errorCode().status().value(),
                ex.errorCode().defaultMessage());
        return error(ex.errorCode(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, ApiFieldError> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            ErrorCode code = resolveFieldErrorCode(error);
            fields.putIfAbsent(error.getField(), new ApiFieldError(code.code(), code.defaultMessage()));
        });
        log.warn("Validation failed on {}: code={}, status={}, fields={}",
                request.getRequestURI(),
                ErrorCode.VALIDATION_ERROR.code(),
                ErrorCode.VALIDATION_ERROR.status().value(),
                fields);
        return error(ErrorCode.VALIDATION_ERROR, request, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, ApiFieldError> fields = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            ErrorCode code = resolveConstraintCode(violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName());
            fields.putIfAbsent(violation.getPropertyPath().toString(), new ApiFieldError(code.code(), code.defaultMessage()));
        });
        log.warn("Constraint validation failed on {}: code={}, status={}, fields={}",
                request.getRequestURI(),
                ErrorCode.VALIDATION_ERROR.code(),
                ErrorCode.VALIDATION_ERROR.status().value(),
                fields);
        return error(ErrorCode.VALIDATION_ERROR, request, fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        Map<String, ApiFieldError> fields = new LinkedHashMap<>();
        Optional<InvalidFormatException> invalidFormatException = invalidFormatException(ex);
        if (invalidFormatException.isPresent()) {
            InvalidFormatException formatException = invalidFormatException.get();
            ErrorCode fieldCode = resolveInvalidFormatCode(formatException);
            fieldName(formatException).ifPresent(field ->
                    fields.put(field, new ApiFieldError(fieldCode.code(), fieldCode.defaultMessage())));
        }

        log.warn("Invalid request body on {}: code={}, status={}, fields={}, reason={}",
                request.getRequestURI(),
                ErrorCode.INVALID_REQUEST_BODY.code(),
                ErrorCode.INVALID_REQUEST_BODY.status().value(),
                fields,
                ex.getMostSpecificCause().getMessage());

        return error(ErrorCode.INVALID_REQUEST_BODY, request, fields);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return error(ErrorCode.INTERNAL_ERROR, request, Map.of());
    }

    private ResponseEntity<ApiErrorResponse> error(
            ErrorCode code,
            HttpServletRequest request,
            Map<String, ApiFieldError> fields
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                code.code(),
                code.defaultMessage(),
                code.status().value(),
                request.getRequestURI(),
                Instant.now(),
                fields
        );
        return ResponseEntity.status(code.status()).body(response);
    }

    private ErrorCode resolveFieldErrorCode(FieldError error) {
        String validationCode = error.getCode();
        if (validationCode == null && error.getCodes() != null && error.getCodes().length > 0) {
            validationCode = error.getCodes()[0];
        }
        return resolveConstraintCode(validationCode);
    }

    private ErrorCode resolveConstraintCode(String validationCode) {
        return switch (validationCode) {
            case "NotBlank", "NotNull" -> ErrorCode.REQUIRED_FIELD;
            case "Email" -> ErrorCode.INVALID_EMAIL_FORMAT;
            case "ValidPassword" -> ErrorCode.INVALID_PASSWORD_POLICY;
            case "Future" -> ErrorCode.FIELD_MUST_BE_FUTURE;
            default -> ErrorCode.INVALID_FIELD_VALUE;
        };
    }

    private ErrorCode resolveInvalidFormatCode(InvalidFormatException ex) {
        if (Instant.class.equals(ex.getTargetType())) {
            return ErrorCode.INVALID_DATE_TIME_FORMAT;
        }
        return ErrorCode.INVALID_FIELD_VALUE;
    }

    private Optional<InvalidFormatException> invalidFormatException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof InvalidFormatException invalidFormatException) {
                return Optional.of(invalidFormatException);
            }
            current = current.getCause();
        }
        return Optional.empty();
    }

    private Optional<String> fieldName(JsonMappingException ex) {
        return ex.getPath()
                .stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst();
    }
}
