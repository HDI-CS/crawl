package kr.co.hdi.user.domain;

import jakarta.persistence.*;
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

    private String password;

    private String name;

    private boolean enabled;

    @Enumerated(STRING)
    private Role role;

    private UserType userType;

    private Boolean surveyDone;

    // V1에서 사용하지 않음
    public static UserEntity createUser(String email, String encodePassword, String name) {
        return UserEntity.builder()
                .enabled(true)
                .role(Role.USER)

                .email(email)
                .password(encodePassword)
                .name(name)

                .surveyDone(false)

                .build();
    }

    public static UserEntity createAdmin(String email, String encodePassword, String name) {
        return UserEntity.builder()
                .enabled(true)
                .role(Role.ADMIN)

                .email(email)
                .password(encodePassword)
                .name(name)

                .build();
    }

    public void updateSurveyDoneStatus() {
        this.surveyDone = true;
    }

    @Builder(access = PRIVATE)
    private UserEntity(String password, boolean enabled, Role role, String email, String name, UserType userType, Boolean surveyDone) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.enabled = enabled;
        this.role = role;
        this.userType = userType;
        this.surveyDone = surveyDone;
    }

}
