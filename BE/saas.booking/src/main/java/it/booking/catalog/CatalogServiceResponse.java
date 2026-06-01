package it.booking.catalog;

public record CatalogServiceResponse(
        Long id,
        Long providerId,
        String name,
        String description,
        int durationMinutes,
        int priceCents
) {
}
