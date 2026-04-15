package kr.co.hdi.admin.user.dto.request;

public record ExpertInfoUpdateRequest(
        String name,
        String phoneNumber,
        String gender,
        String age,
        String career,
        String academic,
        String expertise,
        String company,
        String note,
        String password
) {
}
