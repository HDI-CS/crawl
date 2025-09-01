package kr.co.hdi.global.domain;

import jakarta.persistence.*;
import lombok.Getter;

import static jakarta.persistence.GenerationType.*;

@MappedSuperclass
@Getter
public abstract class Image extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    protected String originalUrl;
    protected String storePath;

}
