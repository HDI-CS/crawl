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
import java.util.Map;

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

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));

        log.info("🔍 핸디스틱청소기 필터 선택 시작");

        try {
            // 1. 체크박스 요소 찾기
            WebElement checkboxElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("chCustom_7971")
            ));

            // 2. 체크박스 현재 상태 확인
            boolean isChecked = checkboxElement.isSelected();
            log.info("   현재 체크박스 상태: {}", isChecked ? "✅ 체크됨" : "⬜ 체크안됨");

            // 3. 체크되어 있지 않은 경우에만 클릭
            if (!isChecked) {
                // 여러 방법으로 클릭 시도
                boolean clickSuccess = false;

                // 방법 1: 체크박스 직접 클릭 (JavaScript)
                try {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    // 체크박스를 체크하고 change 이벤트 발생
                    js.executeScript(
                            "var checkbox = arguments[0];" +
                                    "checkbox.checked = true;" +
                                    "checkbox.dispatchEvent(new Event('change', { bubbles: true }));" +
                                    "checkbox.dispatchEvent(new Event('click', { bubbles: true }));",
                            checkboxElement
                    );
                    Thread.sleep(500);

                    // 체크 상태 재확인
                    isChecked = checkboxElement.isSelected();
                    if (isChecked) {
                        clickSuccess = true;
                        log.info("   ✅ JavaScript로 체크박스 체크 성공");
                    }
                } catch (Exception e) {
                    log.warn("   JavaScript 체크 실패: {}", e.getMessage());
                }

                // 방법 2: label 요소 클릭 (button 때문에 작동 안 할 수 있음)
                if (!clickSuccess) {
                    try {
                        WebElement labelElement = driver.findElement(
                                By.cssSelector("label[for='chCustom_7971']")
                        );

                        // label의 클릭 가능한 영역 찾기 (button 밖의 영역)
                        JavascriptExecutor js = (JavascriptExecutor) driver;
                        js.executeScript(
                                "var label = arguments[0];" +
                                        "var rect = label.getBoundingClientRect();" +
                                        "var x = rect.left + 5;" +  // label의 왼쪽 끝 부분
                                        "var y = rect.top + rect.height / 2;" +
                                        "var clickEvent = new MouseEvent('click', {" +
                                        "    view: window," +
                                        "    bubbles: true," +
                                        "    cancelable: true," +
                                        "    clientX: x," +
                                        "    clientY: y" +
                                        "});" +
                                        "label.dispatchEvent(clickEvent);",
                                labelElement
                        );
                        Thread.sleep(500);

                        isChecked = checkboxElement.isSelected();
                        if (isChecked) {
                            clickSuccess = true;
                            log.info("   ✅ Label 클릭으로 체크 성공");
                        }
                    } catch (Exception e) {
                        log.warn("   Label 클릭 실패: {}", e.getMessage());
                    }
                }

                // 방법 3: 강제로 체크박스 체크 (최후의 수단)
                if (!clickSuccess) {
                    try {
                        JavascriptExecutor js = (JavascriptExecutor) driver;
                        js.executeScript("arguments[0].checked = true;", checkboxElement);
                        Thread.sleep(500);

                        isChecked = checkboxElement.isSelected();
                        if (isChecked) {
                            clickSuccess = true;
                            log.info("   ✅ 강제 체크 성공");
                        }
                    } catch (Exception e) {
                        log.error("   ❌ 모든 체크 시도 실패");
                    }
                }

                if (!clickSuccess) {
                    throw new RuntimeException("체크박스를 체크할 수 없습니다.");
                }

                // 4. 최종 체크 상태 확인
                Thread.sleep(1000);
                isChecked = checkboxElement.isSelected();
                log.info("   최종 체크박스 상태: {}", isChecked ? "✅ 체크됨" : "⬜ 체크안됨");

                if (!isChecked) {
                    log.error("   ❌ 체크박스가 여전히 체크되지 않았습니다!");
                    throw new RuntimeException("체크박스 체크 실패");
                }

                // 5. 필터 적용을 위한 추가 액션 (필요한 경우)
                // 일부 사이트는 체크박스 변경 후 자동으로 적용되지만,
                // 일부는 검색/적용 버튼을 눌러야 함
                try {
                    // 필터 적용을 트리거하는 이벤트 발생
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript(
                            "var event = new Event('change', { bubbles: true });" +
                                    "document.querySelector('.search-box__inner').dispatchEvent(event);"
                    );
                    log.info("   필터 변경 이벤트 발생");
                } catch (Exception e) {
                    // 이벤트 발생 실패는 무시 (자동 적용될 수 있음)
                }
            } else {
                log.info("   ℹ️  이미 체크되어 있음. 추가 작업 불필요");
            }

            // 6. 로딩 대기 및 결과 확인
            log.info("   로딩 대기 중...");
            try {
                // 짧은 대기 후 로더 확인
                Thread.sleep(500);

                // 로더가 나타났다가 사라질 때까지 대기
                try {
                    WebElement loader = shortWait.until(ExpectedConditions.visibilityOfElementLocated(LOADER_LOCATOR));
                    if (loader != null) {
                        log.info("   ⏳ 로딩 중...");
                        wait.until(ExpectedConditions.invisibilityOfElementLocated(LOADER_LOCATOR));
                        log.info("   ✅ 로딩 완료");
                    }
                } catch (Exception e) {
                    // 로더가 없거나 빠르게 사라진 경우
                    log.info("   로더 미감지 (빠른 로딩 또는 자동 적용)");
                }

                // 결과 목록 확인
                wait.until(ExpectedConditions.visibilityOfElementLocated(RESULT_LIST_LOCATOR));

                // 추가 대기
                Thread.sleep(2000);

                // 7. 필터 적용 검증 - 첫 번째 상품 확인
                try {
                    List<WebElement> products = driver.findElements(By.cssSelector("li.prodItem"));
                    if (!products.isEmpty()) {
                        String firstProductText = products.get(0).getText();
                        // 제품명만 추출 (첫 줄)
                        String productName = firstProductText.split("\n")[0];
                        log.info("   🔍 첫 번째 상품: {}", productName);

                        // 샘플로 처음 3개 상품 이름 출력
                        log.info("   📋 상위 3개 상품:");
                        for (int i = 0; i < Math.min(3, products.size()); i++) {
                            String text = products.get(i).getText().split("\n")[0];
                            log.info("      {}. {}", i + 1, text);
                        }
                    }
                } catch (Exception e) {
                    log.warn("   상품 목록 확인 실패: {}", e.getMessage());
                }

                log.info("✅ 핸디스틱청소기 필터 적용 완료");

            } catch (Exception e) {
                log.warn("⚠️  로딩 확인 중 오류: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("❌ 필터 선택 전체 실패: {}", e.getMessage());
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

    @Override
    protected String getProductTypeName() {
        return "핸디스틱청소기";
    }

    @Override
    protected boolean isValidProduct(Map<String, String> productInfo) {
        return true;
    }

}
