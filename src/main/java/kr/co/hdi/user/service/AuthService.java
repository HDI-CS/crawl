package kr.co.hdi.user.service;

import jakarta.transaction.Transactional;
import kr.co.hdi.user.domain.Role;
import kr.co.hdi.user.domain.UserEntity;
import kr.co.hdi.user.dto.response.AuthInfoResponse;
import kr.co.hdi.user.dto.response.AuthResponse;
import kr.co.hdi.user.exception.AuthErrorCode;
import kr.co.hdi.user.exception.AuthException;
import kr.co.hdi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(String email, String password) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthException(AuthErrorCode.INVALID_PASSWORD, "잘못된 비밀번호입니다.");
        }

        return AuthResponse.from(user);
    }

    public AuthResponse createUser(String email, String password, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new AuthException(AuthErrorCode.USER_ALREADY_EXISTS, "이미 존재하는 이메일입니다.");
        }

        UserEntity user = UserEntity.createUser(email, passwordEncoder.encode(password), name);

        userRepository.save(user);
        return AuthResponse.from(user);
    }

    public AuthResponse createAdmin(String email, String password, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new AuthException(AuthErrorCode.USER_ALREADY_EXISTS, "이미 존재하는 이메일입니다.");
        }

        UserEntity user = UserEntity.createAdmin(email, passwordEncoder.encode(password), name);

        userRepository.save(user);
        return AuthResponse.from(user);
    }

    public AuthResponse getAuthInfo(Long userId, String email, String name, Role role) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        return AuthResponse.from(user);
    }
}