package kr.co.hdi.domain.user.entity;

import jakarta.persistence.*;
import kr.co.hdi.admin.user.dto.request.ExpertInfoRequest;
import kr.co.hdi.admin.user.dto.request.ExpertInfoUpdateRequest;
import kr.co.hdi.global.auth.PasswordEncryptConverter;
import kr.co.hdi.global.domain.BaseTimeEntityWithDeletion;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "`user`")
@NoArgsConstructor(access = PROTECTED)
@Getter
public class UserEntity extends BaseTimeEntityWithDeletion {

    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Convert(converter = PasswordEncryptConverter.class)
    private String password;

    private String name;
    private String phoneNumber;
    private String gender;
    private String age;
    private String career;
    private String academic;
    private String expertise;
    private String company;
    private String note;

    private boolean enabled;

    @Enumerated(STRING)
    private Role role;

    @Enumerated(STRING)
    private UserType userType;

    private Boolean surveyDone;

    // V1에서 사용하지 않음
    public static UserEntity createUser(String email, String encodePassword, String name, UserType type) {
        return UserEntity.builder()
                .enabled(true)
                .role(Role.USER)
                .userType(type)

                .email(email)
                .password(encodePassword)
                .name(name)

                .surveyDone(false)

                .build();
    }

    public static UserEntity createAdmin(String email, String encodePassword, String name, UserType type) {
        return UserEntity.builder()
                .enabled(true)
                .role(Role.ADMIN)
                .userType(type)

                .email(email)
                .password(encodePassword)
                .name(name)

                .build();
    }

    public void updateSurveyDoneStatus() {
        this.surveyDone = true;
    }

    public void updateSurveyStatusToFalse() {
        this.surveyDone = false;
    }

    @Builder(access = PRIVATE)
    private UserEntity(
            String email,
            String password,
            String name,
            String phoneNumber,
            String gender,
            String age,
            String career,
            String academic,
            String expertise,
            String company,
            String note,
            boolean enabled,
            Role role,
            UserType userType,
            Boolean surveyDone) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.enabled = enabled;
        this.role = role;
        this.userType = userType;
        this.surveyDone = surveyDone;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.age = age;
        this.career = career;
        this.academic = academic;
        this.expertise = expertise;
        this.company = company;
        this.note = note;
    }

    public static UserEntity createExpert(ExpertInfoRequest request, UserType type, String password) {
        return UserEntity.builder()
                .enabled(true)
                .role(Role.USER)
                .userType(type)

                .name(request.name())
                .email(request.email())
                .password(password)
                .phoneNumber(request.phoneNumber())
                .gender(request.gender())
                .age(request.age())
                .career(request.career())
                .academic(request.academic())
                .expertise(request.expertise())
                .company(request.company())
                .note(request.note())

                .surveyDone(false)

                .build();
    }

    public void updateInfo(ExpertInfoUpdateRequest request) {
        this.name = request.name();
        this.phoneNumber = request.phoneNumber();
        this.gender = request.gender();
        this.age = request.age();
        this.career = request.career();
        this.academic = request.academic();
        this.expertise = request.expertise();
        this.company = request.company();
        this.note = request.note();
        this.password = request.password();
    }
}
