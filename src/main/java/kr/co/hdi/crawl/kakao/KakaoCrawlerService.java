package kr.co.hdi.crawl.kakao;

import com.opencsv.CSVWriter;
import kr.co.hdi.crawl.AbstractBaseCrawler;
import kr.co.hdi.crawl.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class KakaoCrawlerService extends AbstractBaseCrawler {

    @Value("${storage.kakao.brand.output-csv}")
    private String outputCsvPath;
    Set<String> brandNames = new HashSet<>();

    @Override
    protected void crawl() {

        start();
    }

    public void start() {

        List<KakaoBrandListDto> dtos = new ArrayList<>();
        List<CategoryInfo> categoryList = new ArrayList<>(); // 이름+URL 저장용

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // 모든 카테고리 링크 로딩 대기
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector(".list_ctgmain .link_ctg")
        ));

        List<WebElement> categories = driver.findElements(By.cssSelector(".list_ctgmain .link_ctg"));

        // URL + 이름 수집
        for (WebElement category : categories) {
            String categoryName = category.getText().trim(); // 공백 제거
            String categoryUrl = category.getAttribute("href");

            // 제외할 카테고리
            if (categoryName.equals("편의점") ||
                    categoryName.equals("패밀리・호텔뷔페") ||
                    categoryName.equals("퓨전・외국・펍") ||
                    categoryName.equals("와인・양주・맥주") ||
                    categoryName.equals("전체")) {
                continue;
            }

            categoryList.add(new CategoryInfo(categoryName, categoryUrl));
        }

        // URL 기반으로 순회
        for (CategoryInfo cat : categoryList) {
            driver.get(cat.url());

            // 브랜드 탭 요소 대기 후 클릭
            WebElement brandTab = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("span.txt_tab span.txt_g")
            ));
            brandTab.click();

            // next 버튼 다 눌러서 마지막 페이지까지 이동
            while (true) {
                WebElement brandListContainer = wait.until(
                        ExpectedConditions.presenceOfElementLocated(By.cssSelector("app-brand-list"))
                );

                WebElement nextButton = brandListContainer.findElement(By.cssSelector("button.flicking-arrow-next"));
                String disabled = nextButton.getAttribute("aria-disabled");
                if ("true".equals(disabled)) {
                    log.info(cat.name() + " - 다음 버튼 비활성화 됨");
                    break;
                }
                nextButton.click();
            }
            dtos.addAll(getData(cat.name()));
        }
        saveToCsv(dtos, outputCsvPath);
    }

    private List<KakaoBrandListDto> getData(String cg) {

        List<KakaoBrandListDto> dtos = new ArrayList<>();

        WebElement brandListContainer = driver.findElement(By.cssSelector("app-brand-list"));
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("app-brand-list a.link_brand")));
        List<WebElement> brands = brandListContainer.findElements(By.cssSelector("a.link_brand"));

        for (WebElement brand : brands) {

            // 브랜드 로고 이미지
            WebElement logoImg = brand.findElement(By.cssSelector("img.img_g"));
            String logoUrl = logoImg.getAttribute("src");
            String logoAlt = logoImg.getAttribute("alt");

            // 브랜드명
            String brandName = brand.findElement(By.cssSelector(".txt_logo")).getText();

            if (brandNames.contains(logoAlt)) continue;

            brandNames.add(logoAlt);
            dtos.add(new KakaoBrandListDto(logoAlt, logoUrl, cg));
        }
        return dtos;
    }

    private void saveToCsv(List<KakaoBrandListDto>  infoDtos, String filePath) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {

            // 헤더 작성
            String[] header = {"브랜드명", "카테고리", "이미지"};
            writer.writeNext(header);

            // 데이터 작성
            for (KakaoBrandListDto dto : infoDtos) {
                String[] line = {
                        dto.brandName(),
                        dto.category(),
                        dto.brandImage()
                };
                writer.writeNext(line);
            }
        } catch (Exception e) {
            log.error("kakao 선물하기 브랜드 리스트 크롤링 중 에러 발생", e);
        }
    }

    @Override
    public boolean supports(CrawlTarget target) {
        return target.siteType() == SiteType.KAKAO && target.productType() == ProductType.BRAND;
    }
}
