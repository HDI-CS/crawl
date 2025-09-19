package kr.co.hdi.crawl.repository.query;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.co.hdi.crawl.domain.QProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 같은 회사에 유사한 상품명이 존재하는지 확인합니다.
     * @param companyName 회사명
     * @param productName 상품명
     * @return 존재 여부 (true: 존재, false: 없음)
     */
    @Override
    public boolean existsBySimilarProductName(String companyName, String productName, String productPath) {

        QProduct product = QProduct.product;
        String[] words = productName.split("\\s+"); // 공백 기준으로 단어 분리

        BooleanBuilder predicate = new BooleanBuilder();
        for (String word : words) {
            predicate.or(product.productName.containsIgnoreCase(word));
        }

        Integer fetchFirst = queryFactory
                .selectOne()
                .from(product)
                .where(
                        product.companyName.eq(companyName), // 회사명이 같고
                        product.productPath.eq(productPath), // 제품 경로가 같고(종류)
                        predicate
                )
                .fetchFirst();

        return fetchFirst != null;
    }
}
