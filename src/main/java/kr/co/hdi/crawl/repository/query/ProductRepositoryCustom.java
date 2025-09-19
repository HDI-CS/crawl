package kr.co.hdi.crawl.repository.query;

public interface ProductRepositoryCustom {
    boolean existsBySimilarProductName(String companyName, String productName, String productPath);
}
