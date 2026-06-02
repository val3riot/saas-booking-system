package it.booking.catalog;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.booking.common.PageResponse;
import it.booking.config.OpenApiConfig;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/catalog")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Validated
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/providers")
    List<CatalogProviderResponse> listProviders() {
        return catalogService.listProviders();
    }

    @GetMapping("/providers/search")
    PageResponse<CatalogProviderResponse> searchProviders(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate availableOn,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "BUSINESS_NAME") CatalogProviderSort sort,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction
    ) {
        return catalogService.searchProviders(query, category, city, availableOn, page, size, sort, direction);
    }

    @GetMapping("/providers/{providerId}")
    CatalogProviderResponse getProvider(@PathVariable Long providerId) {
        return catalogService.getProvider(providerId);
    }

    @GetMapping("/providers/{providerId}/services")
    List<CatalogServiceResponse> listServices(@PathVariable Long providerId) {
        return catalogService.listServices(providerId);
    }

    @GetMapping("/providers/{providerId}/services/{serviceId}")
    CatalogServiceResponse getService(@PathVariable Long providerId, @PathVariable Long serviceId) {
        return catalogService.getService(providerId, serviceId);
    }
}
