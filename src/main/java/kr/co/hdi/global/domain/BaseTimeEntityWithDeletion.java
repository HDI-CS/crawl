package kr.co.hdi.global.domain;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * soft delete가 필요한 엔티티에서 상속받아 사용
 */
@MappedSuperclass
@Getter
public class BaseTimeEntityWithDeletion extends BaseTimeEntity {

    private LocalDateTime deletedAt;

    protected void processDeletion() {
        this.deletedAt = LocalDateTime.now();
    }
}
