package kr.co.hdi.user.dto.response;


import kr.co.hdi.user.domain.Role;
import kr.co.hdi.user.domain.UserEntity;

public record AuthResponse(
        Long id,
        String email,
        String name,
        Role role
) {

    public static AuthResponse from(UserEntity user
    ) {
        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }

    public static AuthResponse of(Long userId, String email, String name, Role role) {
        return new AuthResponse(userId, email, name, role);
    }
}
