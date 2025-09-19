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

    private String companyName;
    @Column(nullable = false)
    private String productName;

    private String modelName;
    private String price;
    private String material;
    private String size;
    private String weight;
    private String referenceUrl;

    private String registeredAt;
    private String productPath;

    private String productTypeName;


    public static Product from(Map<String, String> productInfo, String referenceUrl) {
        return Product.builder()
                .companyName(productInfo.get("회사명"))
                .productName(productInfo.get("제품명"))
                .modelName(productInfo.get("모델명"))
                .price(productInfo.get("가격"))
                .material("N/A")
                .size(productInfo.get("크기"))
                .weight(productInfo.get("무게"))
                .referenceUrl(referenceUrl)
                .productPath(productInfo.get("제품경로"))
                .registeredAt(productInfo.get("등록일"))
                .productTypeName(productInfo.get("제품유형"))
                .build();
    }

    @Builder(access = PRIVATE)
    private Product(String productName, String modelName, String price, String material, String size, String weight, String referenceUrl, String companyName, String registeredAt, String productPath, String productTypeName) {
        this.productTypeName = productTypeName;
        this.registeredAt = registeredAt;
        this.productPath = productPath;
        this.companyName = companyName;
        this.productName = productName;
        this.modelName = modelName;
        this.price = price;
        this.material = material;
        this.size = size;
        this.weight = weight;
        this.referenceUrl = referenceUrl;
    }
}
