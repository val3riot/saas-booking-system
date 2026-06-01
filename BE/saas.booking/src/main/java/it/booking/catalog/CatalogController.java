package it.booking.catalog;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.booking.config.OpenApiConfig;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/providers")
    List<CatalogProviderResponse> listProviders() {
        return catalogService.listProviders();
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
