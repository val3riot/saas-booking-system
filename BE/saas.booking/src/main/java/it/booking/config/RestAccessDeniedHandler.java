package it.booking.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.booking.common.ApiErrorResponse;
import it.booking.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RestAccessDeniedHandler.class);

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        ErrorCode code = ErrorCode.FORBIDDEN;
        log.warn("Authorization error on {}: code={}, status={}, reason={}",
                request.getRequestURI(),
                code.code(),
                code.status().value(),
                accessDeniedException.getMessage());

        ApiErrorResponse body = new ApiErrorResponse(
                code.code(),
                code.defaultMessage(),
                code.status().value(),
                request.getRequestURI(),
                Instant.now(),
                Map.of()
        );

        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
