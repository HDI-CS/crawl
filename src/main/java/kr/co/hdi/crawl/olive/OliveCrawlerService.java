package kr.co.hdi.crawl.olive;


import kr.co.hdi.crawl.AbstractBaseCrawler;
import kr.co.hdi.crawl.dto.CrawlTarget;
import kr.co.hdi.crawl.dto.ProductType;
import kr.co.hdi.crawl.dto.SiteType;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class OliveCrawlerService extends AbstractBaseCrawler {

    @Override
    protected void crawl() {
        getBrandName();
    }

    private void getBrandName() {

        Set<String> brandSet = new HashSet<>();
        try {
            Actions actions = new Actions(driver);

            // 강제 스크롤 3번
            for(int i = 0; i < 3; i++) {
                actions.sendKeys(Keys.PAGE_DOWN).perform();
                Thread.sleep(1000);
            }

            // 뷰티 탭 클릭
            WebElement beautyButton = driver.findElement(By.xpath("//button[span[text()='뷰티']]"));
            beautyButton.click();
            Thread.sleep(1000);

            while (true) {
                int prevSize = brandSet.size();

                List<WebElement> brandNames = driver.findElements(By.cssSelector(".BrandList_brand-title-wrap___q5zG"));
                for(WebElement brandName : brandNames) {

                    String kr = brandName.findElement(By.cssSelector("span:nth-of-type(1) > div")).getText();
                    String eng = brandName.findElement(By.cssSelector("span:nth-of-type(2)")).getText();
                    brandSet.add(kr + "," + eng);
                }

                if (brandSet.size() == prevSize) {
                    break;
                }

                // 페이지 스크롤
                actions.sendKeys(Keys.PAGE_DOWN).perform();
                Thread.sleep(250);
            }
        } catch (InterruptedException e) {
            log.error("크롤링 중 인터럽트 발생", e);
        } catch (NoSuchElementException e) {
            log.error("요소를 찾을 수 없음", e);
        }

        saveBrandsToCSV(brandSet, "/file/path/brands.csv");  // TODO : file path 수정
    }

    private void saveBrandsToCSV(Set<String>  brandList, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            // 헤더 작성
            writer.write("한글이름,영문이름\n");

            for(String brand : brandList) {
                writer.write(brand+"\n");
            }
            log.info("CSV 저장 완료: {}", filePath);
        } catch (IOException e) {
            log.error("CSV 저장 중 오류 발생", e);
        }
    }

    @Override
    public boolean supports(CrawlTarget target) {
        return target.siteType() == SiteType.OLIVEYOUNG && target.productType() == ProductType.BRAND;
    }

}
