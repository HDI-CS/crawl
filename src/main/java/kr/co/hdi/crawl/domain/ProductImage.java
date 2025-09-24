package kr.co.hdi.crawl.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import kr.co.hdi.global.domain.Image;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@NoArgsConstructor(access = PROTECTED)
@Getter
public class ProductImage extends Image {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id", nullable = false)
    private Product product;

    private String detailPath;
    private String frontPath;
    private String sidePath;

    public static List<ProductImage> createThumbnail(Product product, List<String> imageUrls) {
        return imageUrls.stream()
                .map(url -> createImage(product, url, ImageType.THUMBNAIL))
                .toList();
    }


    public static List<ProductImage> createDetailImage(Product product, List<String> imageUrls) {
        return imageUrls.stream()
                .map(url -> createImage(product, url, ImageType.DETAIL))
                .toList();
    }

    private static ProductImage createImage(Product product, String url, ImageType type) {
        return ProductImage.builder()
                .originalUrl(url)
                .storePath(url) // TODO: S3 저장 경로로 수정
                .product(product)
                .build();
    }

    @Builder(access = PRIVATE)
    private ProductImage(Product product, String originalUrl, String storePath) {
        this.product = product;
        this.originalUrl = originalUrl;
        this.storePath = storePath;
    }
}
