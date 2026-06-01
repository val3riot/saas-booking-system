package it.booking.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR("VAL_000", HttpStatus.BAD_REQUEST, "Validation failed"),
    REQUIRED_FIELD("VAL_001", HttpStatus.BAD_REQUEST, "Required field"),
    INVALID_FIELD_VALUE("VAL_002", HttpStatus.BAD_REQUEST, "Invalid field value"),
    INVALID_EMAIL_FORMAT("VAL_003", HttpStatus.BAD_REQUEST, "Invalid email format"),
    FIELD_MUST_BE_FUTURE("VAL_004", HttpStatus.BAD_REQUEST, "Field must be in the future"),
    INVALID_REQUEST_BODY("VAL_005", HttpStatus.BAD_REQUEST, "Invalid request body"),
    INVALID_DATE_TIME_FORMAT("VAL_006", HttpStatus.BAD_REQUEST, "Invalid date-time format"),
    DATA_INTEGRITY_CONFLICT("DATA_001", HttpStatus.CONFLICT, "Data integrity conflict"),
    UNAUTHORIZED("AUTH_001", HttpStatus.UNAUTHORIZED, "Authentication is required"),
    INVALID_CREDENTIALS("AUTH_002", HttpStatus.UNAUTHORIZED, "Invalid credentials"),
    INVALID_PASSWORD_POLICY("AUTH_003", HttpStatus.BAD_REQUEST, "Invalid password policy"),
    FORBIDDEN("AUTH_004", HttpStatus.FORBIDDEN, "Access denied"),
    EMAIL_ALREADY_REGISTERED("AUTH_005", HttpStatus.CONFLICT, "Email already registered"),
    ACCOUNT_DISABLED("AUTH_006", HttpStatus.FORBIDDEN, "Account disabled"),
    BOOKING_NOT_FOUND("BOOK_001", HttpStatus.NOT_FOUND, "Booking not found"),
    INVALID_BOOKING_INTERVAL("BOOK_002", HttpStatus.BAD_REQUEST, "endsAt must be after startsAt"),
    BOOKING_SLOT_UNAVAILABLE("BOOK_003", HttpStatus.CONFLICT, "Booking slot is not available"),
    PROVIDER_NOT_AVAILABLE("BOOK_004", HttpStatus.CONFLICT, "Provider is not available"),
    SERVICE_NOT_AVAILABLE("BOOK_005", HttpStatus.CONFLICT, "Service is not available"),
    PROVIDER_NOT_FOUND("PROV_001", HttpStatus.NOT_FOUND, "Provider not found"),
    PROVIDER_ALREADY_EXISTS("PROV_002", HttpStatus.CONFLICT, "Provider already exists for user"),
    PROVIDER_USER_ROLE_REQUIRED("PROV_003", HttpStatus.BAD_REQUEST, "User must have PROVIDER role"),
    SERVICE_NOT_FOUND("SERV_001", HttpStatus.NOT_FOUND, "Service not found"),
    SERVICE_ALREADY_EXISTS("SERV_002", HttpStatus.CONFLICT, "Service already exists for provider"),
    AVAILABILITY_NOT_FOUND("AVAIL_001", HttpStatus.NOT_FOUND, "Availability not found"),
    INVALID_AVAILABILITY_INTERVAL("AVAIL_002", HttpStatus.BAD_REQUEST, "endTime must be after startTime"),
    AVAILABILITY_OVERLAP("AVAIL_003", HttpStatus.CONFLICT, "Availability overlaps an existing active slot"),
    USER_NOT_FOUND("USER_001", HttpStatus.NOT_FOUND, "User not found"),
    RESOURCE_NOT_FOUND("RES_001", HttpStatus.NOT_FOUND, "Resource not found"),
    INTERNAL_ERROR("SYS_001", HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
