package it.booking.booking;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.booking.auth.AuthenticatedUser;
import it.booking.config.OpenApiConfig;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/providers/me/bookings")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ProviderBookingController {

    private final BookingService bookingService;

    public ProviderBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    List<BookingResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return bookingService.listForCurrentProvider(user.id());
    }

    @GetMapping("/{bookingId}")
    BookingResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long bookingId) {
        return bookingService.getForCurrentProvider(user.id(), bookingId);
    }

    @PostMapping("/{bookingId}/confirm")
    BookingResponse confirm(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long bookingId) {
        return bookingService.confirmForCurrentProvider(user.id(), bookingId);
    }

    @PostMapping("/{bookingId}/reject")
    BookingResponse reject(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long bookingId,
            @Valid @RequestBody(required = false) RejectBookingRequest request
    ) {
        return bookingService.rejectForCurrentProvider(user.id(), bookingId, request);
    }

    @PostMapping("/{bookingId}/cancel")
    ResponseEntity<Void> cancel(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long bookingId,
            @Valid @RequestBody(required = false) CancelBookingRequest request
    ) {
        bookingService.cancelForCurrentProvider(user.id(), bookingId, request);
        return ResponseEntity.noContent().build();
    }
}
