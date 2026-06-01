package it.booking.catalog;

public record CatalogProviderResponse(
        Long id,
        String businessName,
        String description,
        String category,
        String city,
        String address
) {
}
