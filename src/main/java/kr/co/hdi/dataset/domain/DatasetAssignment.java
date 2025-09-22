package kr.co.hdi.dataset.domain;

import jakarta.persistence.*;
import kr.co.hdi.global.domain.BaseTimeEntityWithDeletion;
import kr.co.hdi.user.domain.UserEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class DatasetAssignment extends BaseTimeEntityWithDeletion {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    private DataType dataType;

    private int startIndex;

    private int endIndex;
}
