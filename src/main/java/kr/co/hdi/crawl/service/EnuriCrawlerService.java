package kr.co.hdi.crawl.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EnuriCrawlerService {

    private WebDriver driver;
    private final String webBaseUrl = "https://www.enuri.com";

    public void startCrawling(String webUrl) {

        initDriver();
        driver.get(webUrl);

        List<String> urls = getProductUrl();
        for (String url : urls) {
            driver.get(webBaseUrl + url);
            getProductData();
        }

        driver.quit();
    }

    private void initDriver() {

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        driver = new ChromeDriver(options);
    }

    private List<String> getProductUrl() {

        List<String> urls = new ArrayList<>();

        WebElement goodsList = driver.findElement(By.cssSelector("div.goods-list"));
        List<WebElement> prodItems = goodsList.findElements(By.cssSelector("li.prodItem"));

        for(WebElement item : prodItems) {

            WebElement itemThumb = item.findElement(By.cssSelector("div.item__thumb"));
            WebElement linkElement = itemThumb.findElement(By.cssSelector("a"));
            String url = linkElement.getDomAttribute("href");
            urls.add(url);
        }
        return urls;
    }

    private void getProductData() {
        getProductInfo();
        getProductImage();
    }

    // TODO : 필요한 정보 파싱 필요
    private void getProductInfo() {

        // 1. 제품 이름
        WebElement prodSum = driver.findElement(By.cssSelector("div.vip-summ__prod"));
        String productName = prodSum.findElement(By.cssSelector("div.vip__tx--title")).getText().trim();
        log.info("프로덕트 이름: {}", productName);

        // 2. 상품 정보
        WebElement specTable = driver.findElement(By.id("enuri_spec_table"));
        List<WebElement> dts = specTable.findElements(By.tagName("dt"));
        List<WebElement> dds = specTable.findElements(By.tagName("dd"));

        Map<String, String> specItems = new HashMap<>();

        for (int i = 0; i < dts.size(); i++) {

            WebElement dd = dds.get(i);
            List<WebElement> rows = dd.findElements(By.tagName("tr"));
            for (WebElement row : rows) {
                List<WebElement> ths = row.findElements(By.tagName("th"));
                List<WebElement> tds = row.findElements(By.tagName("td"));
                for (int j = 0; j < ths.size() && j < tds.size(); j++) {
                    String key = ths.get(j).getText().trim();
                    String value = tds.get(j).getText().trim();
                    specItems.put(key, value);
                }
            }
        }
        specItems.forEach((k, v) -> log.info("{} : {}", k, v));
    }

    private void getProductImage() {
        List<String> imageUrls = new ArrayList<>();

        WebElement thumbList = driver.findElement(By.cssSelector("ul.thum__list"));
        List<WebElement> images = thumbList.findElements(By.tagName("img"));

        for (WebElement img : images) {
            String url = img.getAttribute("src");
            if (url != null && !url.isEmpty()) {
                imageUrls.add(url);
            }
        }
        // TODO: S3 에 저장
//        for(String imageUrl : imageUrls) {
//            System.out.println(imageUrl);
//        }
    }
}
