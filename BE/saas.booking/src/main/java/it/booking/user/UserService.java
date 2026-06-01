package it.booking.user;

import static it.booking.common.EmailUtils.normalize;

import it.booking.common.ApiException;
import it.booking.common.ErrorCode;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(AppUserRepository users, PasswordEncoder passwordEncoder, UserMapper userMapper) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return users.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return users.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        AppUser user = users.save(new AppUser(email, passwordEncoder.encode(request.password()), request.role()));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        AppUser user = users.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        String email = normalize(request.email());
        if (users.existsByEmailAndIdNot(email, id)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        user.update(email, request.role(), request.enabled());
        return userMapper.toResponse(user);
    }

    @Transactional
    public void enable(Long id) {
        AppUser user = users.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        user.enable();
    }

    @Transactional
    public void disable(Long id) {
        AppUser user = users.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        user.disable();
    }

    @Transactional
    public void delete(Long id) {
        disable(id);
    }
}
