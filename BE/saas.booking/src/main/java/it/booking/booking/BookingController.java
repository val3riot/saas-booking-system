package it.booking.booking;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.booking.auth.AuthenticatedUser;
import it.booking.config.OpenApiConfig;
import jakarta.validation.Valid;
import java.net.URI;
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
@RequestMapping("/api/bookings")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    List<BookingResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return bookingService.listForCurrentCustomer(user.id());
    }

    @GetMapping("/{bookingId}")
    BookingResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long bookingId) {
        return bookingService.getForCurrentCustomer(user.id(), bookingId);
    }

    @PostMapping
    ResponseEntity<BookingResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateBookingRequest request
    ) {
        BookingResponse response = bookingService.createForCurrentCustomer(user.id(), request);
        return ResponseEntity.created(URI.create("/api/bookings/" + response.id())).body(response);
    }

    @PostMapping("/{bookingId}/cancel")
    @ApiResponse(responseCode = "204", description = "Booking cancelled")
    ResponseEntity<Void> cancel(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long bookingId,
            @Valid @RequestBody(required = false) CancelBookingRequest request
    ) {
        bookingService.cancelForCurrentCustomer(user.id(), bookingId, request);
        return ResponseEntity.noContent().build();
    }
}
