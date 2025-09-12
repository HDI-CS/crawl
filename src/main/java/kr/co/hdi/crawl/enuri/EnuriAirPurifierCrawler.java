package kr.co.hdi.crawl.enuri;

import kr.co.hdi.crawl.dto.CrawlTarget;
import kr.co.hdi.crawl.dto.ProductType;
import kr.co.hdi.crawl.dto.SiteType;
import kr.co.hdi.crawl.repository.ProductImageRepository;
import kr.co.hdi.crawl.repository.ProductRepositoryCustom;
import org.springframework.stereotype.Service;

@Service
public class EnuriAirPurifierCrawler extends EnuriCrawler{

    public EnuriAirPurifierCrawler(ProductRepositoryCustom productRepository, ProductImageRepository productImageRepository) {
        super(productRepository, productImageRepository);
    }

    @Override
    public boolean supports(CrawlTarget target) {
        return target.siteType() == SiteType.ENURI &&
                target.productType() == ProductType.AIR_PURIFIER;
    }

    @Override
    protected String getCategoryFolderName() {
        return "공기청정기";
    }

    @Override
    protected String getProductPath() {
        return "가전/TV>에어컨/계절가>공기청정/살균기>공기청정기";
    }

    @Override
    protected String getProductTypeName() {
        return "공기청정기";
    }
}
