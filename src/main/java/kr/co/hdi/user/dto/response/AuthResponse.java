package kr.co.hdi.user.dto.response;


import kr.co.hdi.user.domain.Role;
import kr.co.hdi.user.domain.UserEntity;
import kr.co.hdi.user.domain.UserType;

public record AuthResponse(
        Long id,
        String email,
        String name,
        Role role,
        UserType userType,
        Boolean surveyDone
) {

    public static AuthResponse from(UserEntity user
    ) {
        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getUserType(),
                user.getSurveyDone()
        );
    }

    public static AuthResponse of(Long userId, String email, String name, Role role, UserType userType, Boolean surveyDone) {
        return new AuthResponse(userId, email, name, role, userType, surveyDone);
    }
}
