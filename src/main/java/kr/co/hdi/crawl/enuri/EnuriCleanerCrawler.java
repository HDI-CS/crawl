package kr.co.hdi.crawl.enuri;

import kr.co.hdi.crawl.dto.CrawlTarget;
import kr.co.hdi.crawl.dto.ProductType;
import kr.co.hdi.crawl.dto.SiteType;
import kr.co.hdi.crawl.repository.ProductImageRepository;
import kr.co.hdi.crawl.repository.ProductRepositoryCustom;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

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
        WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));

        try {
            // 방법 1: 체크박스 직접 클릭 (가장 확실한 방법)
            WebElement filterElement = null;

            try {
                log.info("방법 1: 핸디스틱청소기 체크박스 직접 찾기 시도");
                // li 요소에서 data-attr 속성으로 찾기
                WebElement liElement = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("li[data-attr='spec_7971_핸디스틱청소기']")));

                // 해당 li 안의 체크박스 찾기
                filterElement = liElement.findElement(By.cssSelector("input[type='checkbox']"));
                log.info("방법 1 성공: 체크박스 요소를 찾았습니다.");
            } catch (Exception e) {
                log.warn("방법 1 실패: {}", e.getMessage());

                // 방법 2: ID로 체크박스 직접 찾기
                try {
                    log.info("방법 2: ID로 체크박스 찾기 시도");
                    filterElement = driver.findElement(By.id("chCustom_7971"));
                    log.info("방법 2 성공: ID로 체크박스를 찾았습니다.");
                } catch (Exception e2) {
                    log.warn("방법 2 실패: {}", e2.getMessage());

                    // 방법 3: label 클릭 (기존 방식)
                    try {
                        log.info("방법 3: label 클릭 시도");
                        filterElement = wait.until(ExpectedConditions.elementToBeClickable(
                                By.cssSelector("label[title='핸디스틱청소기']")));
                        log.info("방법 3 성공: label 요소를 찾았습니다.");
                    } catch (Exception e3) {
                        log.warn("방법 3 실패: {}", e3.getMessage());

                        // 방법 4: button 클릭
                        try {
                            log.info("방법 4: button.btn--dic 클릭 시도");
                            List<WebElement> buttons = driver.findElements(By.cssSelector("button.btn--dic"));
                            for (WebElement button : buttons) {
                                String buttonText = button.getText().trim();
                                String dataKbno = button.getAttribute("data-kbno");
                                log.info("발견된 버튼 - 텍스트: '{}', data-kbno: '{}'", buttonText, dataKbno);

                                if (buttonText.contains("핸디스틱청소기") || "196298".equals(dataKbno)) {
                                    filterElement = button;
                                    log.info("방법 4 성공: 버튼을 찾았습니다 - '{}'", buttonText);
                                    break;
                                }
                            }
                        } catch (Exception e4) {
                            log.warn("방법 4 실패: {}", e4.getMessage());
                        }
                    }
                }
            }

            if (filterElement == null) {
                // 방법 5: 전체 요소 검사
                try {
                    log.info("방법 5: 전체 li 요소에서 핸디스틱청소기 검색");
                    List<WebElement> liElements = driver.findElements(By.cssSelector("li.attrs"));
                    for (WebElement li : liElements) {
                        try {
                            String dataAttr = li.getAttribute("data-attr");
                            String dataSpec = li.getAttribute("data-spec");
                            log.debug("Li 요소 - data-attr: '{}', data-spec: '{}'", dataAttr, dataSpec);

                            if ((dataAttr != null && dataAttr.contains("핸디스틱청소기")) ||
                                    "7971".equals(dataSpec)) {

                                // 체크박스 먼저 시도
                                try {
                                    filterElement = li.findElement(By.cssSelector("input[type='checkbox']"));
                                    log.info("방법 5 성공: 체크박스 찾음 - data-attr: '{}'", dataAttr);
                                    break;
                                } catch (Exception ex) {
                                    // 체크박스가 없으면 label 시도
                                    filterElement = li.findElement(By.cssSelector("label"));
                                    log.info("방법 5 성공: label 찾음 - data-attr: '{}'", dataAttr);
                                    break;
                                }
                            }
                        } catch (Exception liEx) {
                            // 개별 li 처리 오류는 무시하고 계속
                        }
                    }
                } catch (Exception e5) {
                    log.warn("방법 5 실패: {}", e5.getMessage());
                }
            }

            if (filterElement == null) {
                throw new RuntimeException("핸디스틱청소기 필터를 찾을 수 없습니다.");
            }

            // 필터 클릭 시도
            boolean clickSuccess = false;

            // 일반 클릭 시도
            try {
                log.info("일반 클릭 시도");
                filterElement.click();
                clickSuccess = true;
                log.info("일반 클릭 성공");
            } catch (Exception e) {
                log.warn("일반 클릭 실패: {}", e.getMessage());

                // JavaScript 클릭 시도
                try {
                    log.info("JavaScript 클릭 시도");
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("arguments[0].click();", filterElement);
                    clickSuccess = true;
                    log.info("JavaScript 클릭 성공");
                } catch (Exception e2) {
                    log.error("JavaScript 클릭도 실패: {}", e2.getMessage());
                }
            }

            if (!clickSuccess) {
                throw new RuntimeException("필터 클릭에 실패했습니다.");
            }

            // 로딩 대기 및 결과 확인
            try {
                log.info("로딩 완료 대기 중...");
                // 로더가 나타났다가 사라질 때까지 대기
                try {
                    shortWait.until(ExpectedConditions.visibilityOfElementLocated(LOADER_LOCATOR));
                    log.info("로더 감지됨");
                } catch (Exception e) {
                    log.info("로더가 감지되지 않음 (빠르게 처리되었을 수 있음)");
                }

                // 로더가 사라질 때까지 대기
                wait.until(ExpectedConditions.invisibilityOfElementLocated(LOADER_LOCATOR));
                log.info("로더 사라짐");

                // 결과 목록 확인
                wait.until(ExpectedConditions.visibilityOfElementLocated(RESULT_LIST_LOCATOR));
                log.info("결과 목록 로드 완료");

                // 추가 대기 (안정성을 위해)
                Thread.sleep(2000);

                log.info("'핸디스틱청소기' 필터 선택 및 목록 갱신 완료");

            } catch (Exception e) {
                log.warn("로딩 대기 중 오류 발생했지만 계속 진행: {}", e.getMessage());
                // 로딩 대기 실패해도 계속 진행 (이미 로드되었을 수 있음)
            }

        } catch (Exception e) {
            log.error("필터 선택 중 전체적인 오류 발생", e);
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

    @Override
    protected String getProductPath() {
        return "가전/TV>청소기";
    }

}
