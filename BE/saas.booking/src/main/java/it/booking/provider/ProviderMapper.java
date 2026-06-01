package it.booking.provider;

import org.springframework.stereotype.Component;

@Component
public class ProviderMapper {

    public ProviderResponse toResponse(Provider provider) {
        return new ProviderResponse(
                provider.getId(),
                provider.getUser().getId(),
                provider.getBusinessName(),
                provider.getDescription(),
                provider.getCategory(),
                provider.getCity(),
                provider.getAddress(),
                provider.isActive(),
                provider.getCreatedAt(),
                provider.getUpdatedAt()
        );
    }
}
