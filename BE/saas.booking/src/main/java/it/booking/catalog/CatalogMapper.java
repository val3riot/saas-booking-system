package it.booking.catalog;

import it.booking.offering.OfferedService;
import it.booking.provider.Provider;
import org.springframework.stereotype.Component;

@Component
class CatalogMapper {

    CatalogProviderResponse toProviderResponse(Provider provider) {
        return new CatalogProviderResponse(
                provider.getId(),
                provider.getBusinessName(),
                provider.getDescription(),
                provider.getCategory(),
                provider.getCity(),
                provider.getAddress()
        );
    }

    CatalogServiceResponse toServiceResponse(OfferedService service) {
        return new CatalogServiceResponse(
                service.getId(),
                service.getProvider().getId(),
                service.getName(),
                service.getDescription(),
                service.getDurationMinutes(),
                service.getPriceCents()
        );
    }
}
