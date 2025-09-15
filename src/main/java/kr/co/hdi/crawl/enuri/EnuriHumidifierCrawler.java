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
public class EnuriHumidifierCrawler extends EnuriCrawler{

    public EnuriHumidifierCrawler(ProductRepositoryCustom productRepository, ProductImageRepository productImageRepository) {
        super(productRepository, productImageRepository);
    }
    @Override
    protected String getCategoryFolderName() {
        return "가습기";
    }

    @Override
    protected String getProductPath() {
        return "가전/TV>에어컨/계절가전>가습기/에어워셔";
    }

    @Override
    protected String getProductTypeName() {
        return "가습기";
    }

    @Override
    protected boolean isValidProduct(Map<String, String> productInfo) {
        String waterTank = productInfo.get("물통");
        if (waterTank == null || waterTank.isEmpty()) {
            log.warn("물통 정보 없음");
            return false;
        }

        return isValidWaterTankSize(waterTank);
    }

    private boolean isValidWaterTankSize(String waterTank) {
        try {
            Pattern pattern = Pattern.compile("([0-9.]+)L");
            Matcher matcher = pattern.matcher(waterTank);

            if (matcher.find()) {
                double capacity = Double.parseDouble(matcher.group(1));
                return capacity >= 2.0 && capacity <= 5.9;
            }

            // 패턴이 매치되지 않으면 실패로 처리
            log.warn("물통 크기 패턴 매치 실패: {}", waterTank);
            return false;

        } catch (NumberFormatException e) {
            // 숫자 파싱 실패도 실패로 처리
            log.warn("물통 크기 숫자 파싱 실패: {}", waterTank);
            return false;
        }
    }

    @Override
    public boolean supports(CrawlTarget target) {
        return target.siteType() == SiteType.ENURI &&
                target.productType() == ProductType.HUMIDIFIER;
    }
}
