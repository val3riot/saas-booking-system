package it.booking.booking;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.booking.auth.AuthenticatedUser;
import it.booking.config.OpenApiConfig;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/providers/me/agenda")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ProviderAgendaController {

    private final ProviderAgendaService providerAgendaService;

    public ProviderAgendaController(ProviderAgendaService providerAgendaService) {
        this.providerAgendaService = providerAgendaService;
    }

    @GetMapping
    List<BookingResponse> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return providerAgendaService.listForCurrentProvider(user.id(), from, to);
    }
}
