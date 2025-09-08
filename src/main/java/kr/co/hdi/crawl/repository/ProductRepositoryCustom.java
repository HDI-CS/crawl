package kr.co.hdi.crawl.repository;

import kr.co.hdi.crawl.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepositoryCustom extends JpaRepository<Product, Long>, kr.co.hdi.crawl.repository.query.ProductRepositoryCustom {
}
