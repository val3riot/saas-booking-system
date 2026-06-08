package it.booking.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache.ttl")
public class CacheTtlProperties {

    private Duration providerSearch = Duration.ofMinutes(5);
    private Duration providerDetails = Duration.ofMinutes(15);
    private Duration providerServices = Duration.ofMinutes(10);
    private Duration serviceDetails = Duration.ofMinutes(10);

    public Duration getProviderSearch() {
        return providerSearch;
    }

    public void setProviderSearch(Duration providerSearch) {
        this.providerSearch = providerSearch;
    }

    public Duration getProviderDetails() {
        return providerDetails;
    }

    public void setProviderDetails(Duration providerDetails) {
        this.providerDetails = providerDetails;
    }

    public Duration getProviderServices() {
        return providerServices;
    }

    public void setProviderServices(Duration providerServices) {
        this.providerServices = providerServices;
    }

    public Duration getServiceDetails() {
        return serviceDetails;
    }

    public void setServiceDetails(Duration serviceDetails) {
        this.serviceDetails = serviceDetails;
    }
}
