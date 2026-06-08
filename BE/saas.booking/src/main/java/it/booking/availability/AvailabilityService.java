package it.booking.availability;

import it.booking.common.ApiException;
import it.booking.common.ErrorCode;
import it.booking.config.CacheInvalidator;
import it.booking.offering.OfferedService;
import it.booking.offering.OfferedServiceRepository;
import it.booking.provider.Provider;
import it.booking.provider.ProviderRepository;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityService {

    private final AvailabilityRepository availabilities;
    private final ProviderRepository providers;
    private final OfferedServiceRepository offeredServices;
    private final CacheInvalidator cacheInvalidator;
    private final AvailabilityMapper availabilityMapper;

    public AvailabilityService(
            AvailabilityRepository availabilities,
            ProviderRepository providers,
            OfferedServiceRepository offeredServices,
            CacheInvalidator cacheInvalidator,
            AvailabilityMapper availabilityMapper
    ) {
        this.availabilities = availabilities;
        this.providers = providers;
        this.offeredServices = offeredServices;
        this.cacheInvalidator = cacheInvalidator;
        this.availabilityMapper = availabilityMapper;
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> listForCurrentProvider(Long userId, Long serviceId) {
        Provider provider = providerForUser(userId);
        serviceForProvider(serviceId, provider.getId());
        return availabilities.findAllByProviderIdAndServiceIdOrderByDayOfWeekAscStartTimeAsc(provider.getId(), serviceId)
                .stream()
                .map(availabilityMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse getForCurrentProvider(Long userId, Long serviceId, Long availabilityId) {
        Provider provider = providerForUser(userId);
        serviceForProvider(serviceId, provider.getId());
        return availabilities.findByIdAndProviderIdAndServiceId(availabilityId, provider.getId(), serviceId)
                .map(availabilityMapper::toResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.AVAILABILITY_NOT_FOUND));
    }

    @Transactional
    public AvailabilityResponse createForCurrentProvider(Long userId, Long serviceId, CreateAvailabilityRequest request) {
        Provider provider = providerForUserForUpdate(userId);
        OfferedService service = serviceForProvider(serviceId, provider.getId());
        validateInterval(request.startTime(), request.endTime());
        validateNoOverlap(provider.getId(), serviceId, request.dayOfWeek(), request.startTime(), request.endTime());

        Availability availability = availabilities.save(new Availability(
                provider,
                service,
                request.dayOfWeek(),
                request.startTime(),
                request.endTime()
        ));
        cacheInvalidator.serviceChanged(provider.getId(), serviceId);
        return availabilityMapper.toResponse(availability);
    }

    @Transactional
    public AvailabilityResponse updateForCurrentProvider(
            Long userId,
            Long serviceId,
            Long availabilityId,
            UpdateAvailabilityRequest request
    ) {
        Provider provider = providerForUserForUpdate(userId);
        serviceForProvider(serviceId, provider.getId());
        Availability availability = availabilities.findByIdAndProviderIdAndServiceId(availabilityId, provider.getId(), serviceId)
                .orElseThrow(() -> new ApiException(ErrorCode.AVAILABILITY_NOT_FOUND));

        validateInterval(request.startTime(), request.endTime());
        if (request.active()) {
            validateNoOverlap(provider.getId(), serviceId, availabilityId, request.dayOfWeek(), request.startTime(), request.endTime());
        }

        availability.update(request.dayOfWeek(), request.startTime(), request.endTime(), request.active());
        cacheInvalidator.serviceChanged(provider.getId(), serviceId);
        return availabilityMapper.toResponse(availability);
    }

    @Transactional
    public void activateForCurrentProvider(Long userId, Long serviceId, Long availabilityId) {
        Provider provider = providerForUserForUpdate(userId);
        serviceForProvider(serviceId, provider.getId());
        Availability availability = availabilities.findByIdAndProviderIdAndServiceId(availabilityId, provider.getId(), serviceId)
                .orElseThrow(() -> new ApiException(ErrorCode.AVAILABILITY_NOT_FOUND));
        validateNoOverlap(
                provider.getId(),
                serviceId,
                availability.getId(),
                availability.getDayOfWeek(),
                availability.getStartTime(),
                availability.getEndTime()
        );
        availability.activate();
        cacheInvalidator.serviceChanged(provider.getId(), serviceId);
    }

    @Transactional
    public void deactivateForCurrentProvider(Long userId, Long serviceId, Long availabilityId) {
        Provider provider = providerForUser(userId);
        serviceForProvider(serviceId, provider.getId());
        Availability availability = availabilities.findByIdAndProviderIdAndServiceId(availabilityId, provider.getId(), serviceId)
                .orElseThrow(() -> new ApiException(ErrorCode.AVAILABILITY_NOT_FOUND));
        availability.deactivate();
        cacheInvalidator.serviceChanged(provider.getId(), serviceId);
    }

    @Transactional
    public void deleteForCurrentProvider(Long userId, Long serviceId, Long availabilityId) {
        deactivateForCurrentProvider(userId, serviceId, availabilityId);
    }

    private Provider providerForUser(Long userId) {
        return providers.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));
    }

    private Provider providerForUserForUpdate(Long userId) {
        return providers.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));
    }

    private OfferedService serviceForProvider(Long serviceId, Long providerId) {
        return offeredServices.findByIdAndProviderId(serviceId, providerId)
                .orElseThrow(() -> new ApiException(ErrorCode.SERVICE_NOT_FOUND));
    }

    private void validateInterval(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new ApiException(ErrorCode.INVALID_AVAILABILITY_INTERVAL);
        }
    }

    private void validateNoOverlap(Long providerId, Long serviceId, short dayOfWeek, LocalTime startTime, LocalTime endTime) {
        if (availabilities.existsActiveOverlap(providerId, serviceId, dayOfWeek, startTime, endTime)) {
            throw new ApiException(ErrorCode.AVAILABILITY_OVERLAP);
        }
    }

    private void validateNoOverlap(Long providerId, Long serviceId, Long availabilityId, short dayOfWeek, LocalTime startTime, LocalTime endTime) {
        if (availabilities.existsActiveOverlapExcludingId(providerId, serviceId, availabilityId, dayOfWeek, startTime, endTime)) {
            throw new ApiException(ErrorCode.AVAILABILITY_OVERLAP);
        }
    }
}
