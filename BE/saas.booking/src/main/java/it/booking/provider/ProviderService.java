package it.booking.provider;

import static it.booking.common.TextUtils.trim;
import static it.booking.common.TextUtils.trimNullable;

import it.booking.common.ApiException;
import it.booking.common.ErrorCode;
import it.booking.user.AppUser;
import it.booking.user.AppUserRepository;
import it.booking.user.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderService {

    private final ProviderRepository providers;
    private final AppUserRepository users;
    private final ProviderMapper providerMapper;

    public ProviderService(ProviderRepository providers, AppUserRepository users, ProviderMapper providerMapper) {
        this.providers = providers;
        this.users = users;
        this.providerMapper = providerMapper;
    }

    @Transactional(readOnly = true)
    public List<ProviderResponse> list() {
        return providers.findAllByOrderByBusinessNameAsc()
                .stream()
                .map(providerMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderResponse get(Long id) {
        return providers.findById(id)
                .map(providerMapper::toResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public ProviderResponse getByUserId(Long userId) {
        return getProviderByUserId(userId)
                .map(providerMapper::toResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));
    }

    @Transactional
    public ProviderResponse createByUserId(Long userId, CreateProviderProfileRequest request) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        validateProviderUser(user);

        if (providers.existsByUserId(userId)) {
            throw new ApiException(ErrorCode.PROVIDER_ALREADY_EXISTS);
        }

        Provider provider = providers.save(new Provider(
                user,
                trim(request.businessName()),
                trimNullable(request.description()),
                trim(request.category()),
                trim(request.city()),
                trimNullable(request.address())
        ));
        return providerMapper.toResponse(provider);
    }

    @Transactional
    public ProviderResponse create(CreateProviderRequest request) {
        AppUser user = users.findById(request.userId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        validateProviderUser(user);

        if (providers.existsByUserId(request.userId())) {
            throw new ApiException(ErrorCode.PROVIDER_ALREADY_EXISTS);
        }

        Provider provider = providers.save(new Provider(
                user,
                trim(request.businessName()),
                trimNullable(request.description()),
                trim(request.category()),
                trim(request.city()),
                trimNullable(request.address())
        ));
        return providerMapper.toResponse(provider);
    }

    @Transactional
    public ProviderResponse update(Long id, UpdateProviderRequest request) {
        Provider provider = providers.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));
        AppUser user = users.findById(request.userId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        validateProviderUser(user);

        if (providers.existsByUserIdAndIdNot(request.userId(), id)) {
            throw new ApiException(ErrorCode.PROVIDER_ALREADY_EXISTS);
        }

        if (!provider.getUser().getId().equals(user.getId())) {
            provider.changeUser(user);
        }

        provider.update(
                trim(request.businessName()),
                trimNullable(request.description()),
                trim(request.category()),
                trim(request.city()),
                trimNullable(request.address()),
                request.active()
        );
        return providerMapper.toResponse(provider);
    }

    @Transactional
    public ProviderResponse updateByUserId(Long userId, UpdateProviderProfileRequest request) {
        Provider provider = getProviderByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));

        provider.update(
                trim(request.businessName()),
                trimNullable(request.description()),
                trim(request.category()),
                trim(request.city()),
                trimNullable(request.address()),
                provider.isActive()
        );
        return providerMapper.toResponse(provider);
    }

    @Transactional
    public void activate(Long id) {
        Provider provider = providers.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));
        provider.activate();
    }

    @Transactional
    public void deactivate(Long id) {
        Provider provider = providers.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));
        provider.deactivate();
    }

    @Transactional
    public void delete(Long id) {
        deactivate(id);
    }

    private void validateProviderUser(AppUser user) {
        if (user.getRole() != UserRole.PROVIDER) {
            throw new ApiException(ErrorCode.PROVIDER_USER_ROLE_REQUIRED);
        }
    }

    private Optional<Provider> getProviderByUserId(Long userId) {
        return providers.findByUserId(userId);
    }
}
