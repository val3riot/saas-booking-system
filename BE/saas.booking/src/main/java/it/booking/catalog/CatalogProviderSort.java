package it.booking.catalog;

public enum CatalogProviderSort {
    BUSINESS_NAME("businessName"),
    CITY("city"),
    CATEGORY("category");

    private final String property;

    CatalogProviderSort(String property) {
        this.property = property;
    }

    String property() {
        return property;
    }
}
