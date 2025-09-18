package kr.co.hdi.user.dto.response;


import kr.co.hdi.user.domain.Role;

public record AuthResponse(
        Long id,
        String email,
        Role role
) {
}
