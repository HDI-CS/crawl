package kr.co.hdi.user.dto.response;

import kr.co.hdi.user.domain.UserType;

public record AuthInfoResponse(
        UserType userType,
        Boolean surveyDone
) {
}
