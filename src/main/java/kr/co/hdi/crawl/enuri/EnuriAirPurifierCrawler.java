package kr.co.hdi.crawl.enuri;

import kr.co.hdi.crawl.dto.CrawlTarget;
import kr.co.hdi.crawl.dto.ProductType;
import kr.co.hdi.crawl.dto.SiteType;
import kr.co.hdi.crawl.repository.ProductImageRepository;
import kr.co.hdi.crawl.repository.ProductRepositoryCustom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
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

    /**
     * 공기청정기는 수량이 부족해 일단 true 반환하도록
     * @param productInfo 상품 정보 맵
     * @return 유효한 상품인지 여부
     */
    @Override
    protected boolean isValidProduct(Map<String, String> productInfo) {
        // 사용면적 확인
        String usageArea = productInfo.get("사용면적");
        if (usageArea != null) {
            return isValidUsageArea(usageArea);
        }
        // 사용면적 정보가 없으면
        return true;
    }

    private boolean isValidUsageArea(String usageArea) {
        try {
            // "13평(43㎡)" 형태에서 평수 추출
            Pattern pattern = Pattern.compile("(\\d+)평");
            Matcher matcher = pattern.matcher(usageArea);

            if (matcher.find()) {
                int area = Integer.parseInt(matcher.group(1));
                return area >= 10 && area <= 79; // 10평 이상 79평 이하만 통과
            }
        } catch (Exception e) {
            log.warn("사용면적 파싱 실패: {}", usageArea);
        }

        return true; // 파싱 실패시 통과
    }





}
