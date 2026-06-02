package it.booking.catalog;

import it.booking.common.ApiException;
import it.booking.common.ErrorCode;
import it.booking.common.PageResponse;
import it.booking.common.TextUtils;
import it.booking.offering.OfferedServiceRepository;
import it.booking.provider.ProviderRepository;
import java.time.LocalDate;
import java.util.List;
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
        return providers.findAllByActiveTrueOrderByBusinessNameAsc()
                .stream()
                .map(catalogMapper::toProviderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
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
    public CatalogProviderResponse getProvider(Long providerId) {
        return providers.findByIdAndActiveTrue(providerId)
                .map(catalogMapper::toProviderResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<CatalogServiceResponse> listServices(Long providerId) {
        ensureActiveProvider(providerId);
        return offeredServices.findAllByProviderIdAndActiveTrueOrderByNameAsc(providerId)
                .stream()
                .map(catalogMapper::toServiceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CatalogServiceResponse getService(Long providerId, Long serviceId) {
        ensureActiveProvider(providerId);
        return offeredServices.findByIdAndProviderIdAndActiveTrue(serviceId, providerId)
                .map(catalogMapper::toServiceResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.SERVICE_NOT_FOUND));
    }

    private void ensureActiveProvider(Long providerId) {
        if (!providers.existsByIdAndActiveTrue(providerId)) {
            throw new ApiException(ErrorCode.PROVIDER_NOT_FOUND);
        }
    }
}
