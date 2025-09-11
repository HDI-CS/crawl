package kr.co.hdi.crawl.enuri;

import kr.co.hdi.crawl.dto.CrawlTarget;
import kr.co.hdi.crawl.dto.ProductType;
import kr.co.hdi.crawl.dto.SiteType;
import kr.co.hdi.crawl.repository.ProductImageRepository;
import kr.co.hdi.crawl.repository.ProductRepositoryCustom;
import org.springframework.stereotype.Service;

@Service
public class EnuriHairDryerCrawler extends EnuriCrawler {

    public EnuriHairDryerCrawler(ProductRepositoryCustom productRepository, ProductImageRepository productImageRepository) {
        super(productRepository, productImageRepository);
    }

    @Override
    public boolean supports(CrawlTarget target) {
        return target.siteType() == SiteType.ENURI &&
                target.productType() == ProductType.HAIR_DRYER;
    }

    @Override
    protected String getCategoryFolderName() {
        return "헤어드리아어";
    }

    @Override
    protected String getProductPath() {
        return "가전/TV>이미용/소형가전>헤어기기/두피관리기>드라이기";
    }
}
