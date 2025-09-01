package kr.co.hdi.crawl.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "product")
@NoArgsConstructor(access = PROTECTED)
@Getter
public class Product {

    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productName;

    private String modelName;
    private String price;
    private String material;
    private String size;
    private String weight;

    public static Product from(Map<String, String> productInfo) {
        return Product.builder()
                .productName(productInfo.get("프로덕트 이름"))
                .modelName("N/A")
                .price(productInfo.get("가격"))
                .material("N/A")
                .size(productInfo.get("크기"))
                .weight(productInfo.get("무게"))
                .build();
    }

    @Builder(access = PRIVATE)
    private Product(String productName, String modelName, String price, String material, String size, String weight) {
        this.productName = productName;
        this.modelName = modelName;
        this.price = price;
        this.material = material;
        this.size = size;
        this.weight = weight;
    }
}
