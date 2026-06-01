package it.booking.offering;

import static it.booking.common.TextUtils.trim;
import static it.booking.common.TextUtils.trimNullable;

import it.booking.common.ApiException;
import it.booking.common.ErrorCode;
import it.booking.provider.Provider;
import it.booking.provider.ProviderRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfferedServiceService {

    private final OfferedServiceRepository offeredServices;
    private final ProviderRepository providers;
    private final OfferedServiceMapper offeredServiceMapper;

    public OfferedServiceService(
            OfferedServiceRepository offeredServices,
            ProviderRepository providers,
            OfferedServiceMapper offeredServiceMapper
    ) {
        this.offeredServices = offeredServices;
        this.providers = providers;
        this.offeredServiceMapper = offeredServiceMapper;
    }

    @Transactional(readOnly = true)
    public List<OfferedServiceResponse> listForCurrentProvider(Long userId) {
        Provider provider = providerForUser(userId);
        return offeredServices.findAllByProviderIdOrderByNameAsc(provider.getId())
                .stream()
                .map(offeredServiceMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OfferedServiceResponse getForCurrentProvider(Long userId, Long serviceId) {
        Provider provider = providerForUser(userId);
        return serviceForProvider(provider.getId(), serviceId);
    }

    @Transactional
    public OfferedServiceResponse createForCurrentProvider(Long userId, CreateOfferedServiceRequest request) {
        Provider provider = providerForUser(userId);
        String name = trim(request.name());
        if (offeredServices.existsByProviderIdAndNameIgnoreCase(provider.getId(), name)) {
            throw new ApiException(ErrorCode.SERVICE_ALREADY_EXISTS);
        }

        OfferedService service = offeredServices.save(new OfferedService(
                provider,
                name,
                trimNullable(request.description()),
                request.durationMinutes(),
                request.priceCents()
        ));
        return offeredServiceMapper.toResponse(service);
    }

    @Transactional
    public OfferedServiceResponse updateForCurrentProvider(
            Long userId,
            Long serviceId,
            UpdateOfferedServiceRequest request
    ) {
        Provider provider = providerForUser(userId);
        OfferedService service = offeredServices.findByIdAndProviderId(serviceId, provider.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.SERVICE_NOT_FOUND));
        String name = trim(request.name());

        if (offeredServices.existsByProviderIdAndNameIgnoreCaseAndIdNot(provider.getId(), name, serviceId)) {
            throw new ApiException(ErrorCode.SERVICE_ALREADY_EXISTS);
        }

        service.update(
                name,
                trimNullable(request.description()),
                request.durationMinutes(),
                request.priceCents(),
                request.active()
        );
        return offeredServiceMapper.toResponse(service);
    }

    @Transactional
    public void activateForCurrentProvider(Long userId, Long serviceId) {
        Provider provider = providerForUser(userId);
        OfferedService service = offeredServices.findByIdAndProviderId(serviceId, provider.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.SERVICE_NOT_FOUND));
        service.activate();
    }

    @Transactional
    public void deactivateForCurrentProvider(Long userId, Long serviceId) {
        Provider provider = providerForUser(userId);
        OfferedService service = offeredServices.findByIdAndProviderId(serviceId, provider.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.SERVICE_NOT_FOUND));
        service.deactivate();
    }

    @Transactional
    public void deleteForCurrentProvider(Long userId, Long serviceId) {
        deactivateForCurrentProvider(userId, serviceId);
    }

    private OfferedServiceResponse serviceForProvider(Long providerId, Long serviceId) {
        return offeredServices.findByIdAndProviderId(serviceId, providerId)
                .map(offeredServiceMapper::toResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.SERVICE_NOT_FOUND));
    }

    private Provider providerForUser(Long userId) {
        return providers.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));
    }
}
