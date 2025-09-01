package kr.co.hdi.crawl.repository;

import kr.co.hdi.crawl.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
