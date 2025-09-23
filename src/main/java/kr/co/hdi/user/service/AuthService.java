package kr.co.hdi.user.service;

import jakarta.transaction.Transactional;
import kr.co.hdi.user.domain.UserEntity;
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

    public AuthResponse loginOrRegister(String email, String password) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthException(AuthErrorCode.INVALID_PASSWORD, "잘못된 비밀번호입니다.");
        }

        return AuthResponse.from(user);
    }
}