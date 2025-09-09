package kr.co.hdi.crawl.enuri;

import kr.co.hdi.crawl.dto.CrawlTarget;
import kr.co.hdi.crawl.dto.ProductType;
import kr.co.hdi.crawl.dto.SiteType;
import kr.co.hdi.crawl.repository.ProductImageRepository;
import kr.co.hdi.crawl.repository.ProductRepositoryCustom;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class EnuriCleanerCrawler extends EnuriCrawler{

    public EnuriCleanerCrawler(ProductRepositoryCustom productRepository, ProductImageRepository productImageRepository) {
        super(productRepository, productImageRepository);
    }

    @Override
    protected void applyCustomLogic() {
        selectCleanerFilter();
    }

    /**
     * '핸디스틱청소기' 필터를 선택하고 상품 목록이 갱신될 때까지 대기합니다.
     */
    private void selectCleanerFilter() {
        final By LOADER_LOCATOR = By.cssSelector("div.comm-loader");
        final By RESULT_LIST_LOCATOR = By.cssSelector("div.goods-list");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        try {
            WebElement filterLabel = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("label[title='핸디스틱청소기']")));
            filterLabel.click();
            log.info("'핸디스틱청소기' 필터를 클릭했습니다.");

            wait.until(ExpectedConditions.invisibilityOfElementLocated(LOADER_LOCATOR));
            wait.until(ExpectedConditions.visibilityOfElementLocated(RESULT_LIST_LOCATOR));

            log.info("상품 목록이 성공적으로 갱신되었습니다.");

        } catch (Exception e) {
            log.error("필터 선택 또는 목록 갱신 대기 중 오류 발생", e);
            throw new RuntimeException("필터링에 실패하여 크롤링을 중단합니다.", e);
        }
    }

    @Override
    public boolean supports(CrawlTarget target) {
        return target.siteType() == SiteType.ENURI && target.productType() == ProductType.CLEANER;
    }


    @Override
    protected String getCategoryFolderName() {
        return "청소기";
    }

    @Override
    protected String getVerificationKeyword() {
        return "핸디스틱청소기";
    }

}
