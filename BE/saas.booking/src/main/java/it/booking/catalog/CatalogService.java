package it.booking.catalog;

import static it.booking.config.CacheNames.PROVIDER_DETAILS;
import static it.booking.config.CacheNames.PROVIDER_SEARCH;
import static it.booking.config.CacheNames.PROVIDER_SERVICES;
import static it.booking.config.CacheNames.SERVICE_DETAILS;

import it.booking.common.ApiException;
import it.booking.common.ErrorCode;
import it.booking.common.PageResponse;
import it.booking.common.TextUtils;
import it.booking.offering.OfferedServiceRepository;
import it.booking.provider.ProviderRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {

    private final ProviderRepository providers;
    private final OfferedServiceRepository offeredServices;
    private final CatalogMapper catalogMapper;

    public CatalogService(
            ProviderRepository providers,
            OfferedServiceRepository offeredServices,
            CatalogMapper catalogMapper
    ) {
        this.providers = providers;
        this.offeredServices = offeredServices;
        this.catalogMapper = catalogMapper;
    }

    @Transactional(readOnly = true)
    public List<CatalogProviderResponse> listProviders() {
        return providers.findAll(CatalogProviderSpecifications.search(null, null, null, null), Sort.by(Sort.Direction.ASC, "businessName"))
                .stream()
                .map(catalogMapper::toProviderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = PROVIDER_SEARCH,
            key = "'default'",
            condition = "@providerSearchCachePolicy.isBaseSearch("
                    + "#query, #category, #city, #availableOn, #page, #size, #sort, #direction)"
    )
    public PageResponse<CatalogProviderResponse> searchProviders(
            String query,
            String category,
            String city,
            LocalDate availableOn,
            int page,
            int size,
            CatalogProviderSort sort,
            Sort.Direction direction
    ) {
        Short dayOfWeek = availableOn == null ? null : (short) availableOn.getDayOfWeek().getValue();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sort.property()));
        Page<CatalogProviderResponse> result = providers.findAll(
                        CatalogProviderSpecifications.search(
                                TextUtils.trimNullable(query),
                                TextUtils.trimNullable(category),
                                TextUtils.trimNullable(city),
                                dayOfWeek
                        ),
                        pageRequest
                )
                .map(catalogMapper::toProviderResponse);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = PROVIDER_DETAILS, key = "#providerId")
    public CatalogProviderResponse getProvider(Long providerId) {
        return providers.findByIdAndActiveTrueAndUserEnabledTrue(providerId)
                .map(catalogMapper::toProviderResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = PROVIDER_SERVICES, key = "#providerId")
    public List<CatalogServiceResponse> listServices(Long providerId) {
        ensureActiveProvider(providerId);
        return new ArrayList<>(offeredServices.findAllBookableByProviderIdOrderByNameAsc(providerId)
                .stream()
                .map(catalogMapper::toServiceResponse)
                .toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = SERVICE_DETAILS)
    public CatalogServiceResponse getService(Long providerId, Long serviceId) {
        ensureActiveProvider(providerId);
        return offeredServices.findBookableByIdAndProviderId(serviceId, providerId)
                .map(catalogMapper::toServiceResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.SERVICE_NOT_FOUND));
    }

    private void ensureActiveProvider(Long providerId) {
        if (!providers.existsByIdAndActiveTrueAndUserEnabledTrue(providerId)) {
            throw new ApiException(ErrorCode.PROVIDER_NOT_FOUND);
        }
    }
}
