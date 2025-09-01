package kr.co.hdi.crawl.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import kr.co.hdi.crawl.domain.Product;
import kr.co.hdi.crawl.domain.ProductImage;
import kr.co.hdi.crawl.repository.ProductImageRepository;
import kr.co.hdi.crawl.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class EnuriCrawlerService {

    private WebDriver driver;
    private final String webBaseUrl = "https://www.enuri.com";

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

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

        // user agent 설정
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";
        options.addArguments("--user-agent=" + userAgent);
        // 창이 뜨지 않도록 headless 모드 설정
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("lang=ko_KR");
        options.addArguments("--disable-blink-features=AutomationControlled");

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
        Map<String, String> productInfo = getProductInfo();
        List<String> productImages = getProductImage();
        saveProduct(productInfo, productImages);
    }

    private void saveProduct(Map<String, String> productInfo, List<String> productImages) {
        Product product = Product.from(productInfo);
        productRepository.save(product);

        List<ProductImage> images = ProductImage.from(product, productImages);
        productImageRepository.saveAll(images);
    }

    private Map<String, String> getProductInfo() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // 1. 제품 이름
        WebElement prodSum = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.vip-summ__prod")));
        String productName = prodSum.findElement(By.cssSelector("div.vip__tx--title")).getText().trim();
        log.info("프로덕트 이름: {}", productName);

        // 2. 상품 정보
        WebElement specTable = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("enuri_spec_table")));
        List<WebElement> dts = specTable.findElements(By.tagName("dt"));
        List<WebElement> dds = specTable.findElements(By.tagName("dd"));

        Map<String, String> specItems = new HashMap<>();

        // 가격
        WebElement prodPrice = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.prodminprice__tx--price")));
        String price = prodPrice.findElement(By.cssSelector("strong")).getText().trim();

        specItems.put("가격", price);

        specItems.put("프로덕트 이름", productName);
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
        return specItems;
    }

    private List<String> getProductImage() {
        List<String> imageUrls = new ArrayList<>();

        WebElement thumbList = driver.findElement(By.cssSelector("ul.thum__list"));
        List<WebElement> images = thumbList.findElements(By.tagName("img"));

        for (WebElement img : images) {
            String url = img.getAttribute("src");
            if (url != null && !url.isEmpty() && !url.contains("youtube.com")) {
                imageUrls.add(url);
                log.info("이미지 URL: {}", url);
            }
        }
        return imageUrls;
        // TODO: S3 에 저장
//        for(String imageUrl : imageUrls) {
//            System.out.println(imageUrl);
//        }
    }
}
