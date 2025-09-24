package kr.co.hdi.crawl.repository;

import kr.co.hdi.crawl.domain.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    ProductImage findByProductId(Long productId);
}
