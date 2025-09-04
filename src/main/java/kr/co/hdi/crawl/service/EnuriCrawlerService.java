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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class EnuriCrawlerService {

    private WebDriver driver;
    private static final String webBaseUrl = "https://www.enuri.com";

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    @Value("${etc.local-image-path:./images}")
    private String imageStoragePath;

    public void startCrawling(String webUrl) {

        try {
            initDriver();
            driver.get(webUrl);

//            selectCleanerFilter();

            List<String> urls = getProductUrl();
            for (String url : urls) {
                driver.get(webBaseUrl + url);
                getProductData(webBaseUrl + url);
            }
        } finally {
            driver.quit();
        }
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

    private void getProductData(String productUrl) {
        Map<String, String> productInfo = getProductInfo();
        List<String> productImages = getProductImage();
        List<String> detailImage = getProductDetailImage();
        saveProduct(productInfo, productImages, detailImage, productUrl);
    }



    @Transactional
    protected void saveProduct(Map<String, String> productInfo, List<String> productImages, List<String> detailImages, String productUrl) {
        Product product = Product.from(productInfo, productUrl);
        productRepository.save(product);

        String productFolderPath = createProductFolder(product.getId(), product.getProductName());

        List<String> localImagePaths = downloadImagesToLocal(productImages, productFolderPath, "thumbnails");
        log.info("상품 이미지 경로: {}", localImagePaths);

        List<String> detailLocalImagePaths = downloadImagesToLocal(detailImages, productFolderPath, "details");
        log.info("상세 이미지 경로: {}", detailLocalImagePaths);

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

    private List<String> getProductDetailImage() {

        List<String> imageUrls = new ArrayList<>();

        try {
            // 첫 번째 시도: div.thum_wrap에서 이미지 찾기
            WebElement detailElement = driver.findElement(By.cssSelector("div.thum_wrap"));
            List<WebElement> images = detailElement.findElements(By.tagName("img"));

            for (WebElement image : images) {
                String url = image.getAttribute("src");
                if (url != null && !url.isEmpty()) {
                    imageUrls.add(url);
                    log.info("상세 이미지 URL (thum_wrap): {}", url);
                }
            }
        } catch (Exception e) {
            log.warn("div.thum_wrap을 찾을 수 없습니다. 대안 방법으로 시도합니다: {}", e.getMessage());

            try {
                // 두 번째 시도: div.tx_wrap.cw__cont에서 p 태그 내 img 찾기
                WebElement txWrapElement = driver.findElement(By.cssSelector("div.tx_wrap.cw__cont"));
                List<WebElement> pTags = txWrapElement.findElements(By.tagName("p"));

                for (WebElement pTag : pTags) {
                    List<WebElement> images = pTag.findElements(By.tagName("img"));
                    for (WebElement image : images) {
                        String url = image.getAttribute("src");
                        if (url != null && !url.isEmpty()) {
                            imageUrls.add(url);
                            log.info("상세 이미지 URL (tx_wrap): {}", url);
                        }
                    }
                }
            } catch (Exception ex) {
                log.error("상세 이미지 요소를 찾을 수 없습니다: {}", ex.getMessage());
            }
        }
        log.info("총 {}개의 상세 이미지를 찾았습니다.", imageUrls.size());
        return imageUrls;
    }


    private List<String> downloadImagesToLocal(List<String> imageUrls, String productFolderPath, String subFolderName) {
        List<String> localPaths = new ArrayList<>();

        // 하위 폴더 생성 (thumbnails 또는 details)
        String subFolderPath = createSubFolder(productFolderPath, subFolderName);

        for (int i = 0; i < imageUrls.size(); i++) {
            String imageUrl = imageUrls.get(i);
            try {
                String localPath = downloadImage(imageUrl, subFolderPath, i, subFolderName);
                if (localPath != null) {
                    localPaths.add(localPath);
                    log.info("{} 이미지 다운로드 완료: {}", subFolderName, localPath);
                }
            } catch (Exception e) {
                log.error("{} 이미지 다운로드 실패: {} - {}", subFolderName, imageUrl, e.getMessage());
            }
        }

        return localPaths;
    }

    /**
     * 상품별 메인 폴더 생성
     */
    private String createProductFolder(Long productId, String productName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 파일명에 사용할 수 없는 문자 제거
//        String sanitizedProductName = sanitizeFileName(productName);

        String folderPath = Paths.get(imageStoragePath, productName + "_" + timestamp).toString();

        try {
            Path path = Paths.get(folderPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("제품 폴더 생성: {}", folderPath);
            }
        } catch (IOException e) {
            log.error("제품 폴더 생성 실패: {} - {}", folderPath, e.getMessage());
        }

        return folderPath;
    }

    /**
     * 하위 폴더 생성 (thumbnails, details)
     */
    private String createSubFolder(String parentFolderPath, String subFolderName) {
        String subFolderPath = Paths.get(parentFolderPath, subFolderName).toString();

        try {
            Path path = Paths.get(subFolderPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("{} 하위 폴더 생성: {}", subFolderName, subFolderPath);
            }
        } catch (IOException e) {
            log.error("{} 하위 폴더 생성 실패: {} - {}", subFolderName, subFolderPath, e.getMessage());
        }

        return subFolderPath;
    }

    /**
     * 개별 이미지 다운로드
     */
    private String downloadImage(String imageUrl, String folderPath, int index, String prefix) throws IOException {
        // URL에서 파일 확장자 추출
        String fileExtension = getFileExtension(imageUrl);
        String fileName = String.format("%s_%03d%s", prefix, index + 1, fileExtension);
        String filePath = Paths.get(folderPath, fileName).toString();

        HttpURLConnection connection = null;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            // User-Agent 설정 (일부 사이트에서 필요)
            connection.setRequestProperty("User-Agent",
                                          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                outputStream = new FileOutputStream(filePath);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                return filePath;
            } else {
                log.warn("이미지 다운로드 실패 - HTTP 상태코드: {} URL: {}", responseCode, imageUrl);
                return null;
            }

        } finally {
            if (outputStream != null) {
                try { outputStream.close(); } catch (IOException e) { /* ignore */ }
            }
            if (inputStream != null) {
                try { inputStream.close(); } catch (IOException e) { /* ignore */ }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    /**
     * URL에서 파일 확장자 추출
     */
    private String getFileExtension(String imageUrl) {
        try {
            String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);

            if (fileName.contains("?")) {
                fileName = fileName.substring(0, fileName.indexOf("?"));
            }

            if (fileName.contains(".")) {
                String extension = fileName.substring(fileName.lastIndexOf("."));
                if (extension.matches("\\.(jpg|jpeg|png|gif|webp|bmp)$")) {
                    return extension;
                }
            }

            return ".jpg";
        } catch (Exception e) {
            return ".jpg";
        }
    }

    /**
     * '핸디스틱청소기' 필터를 선택하고 상품 목록이 갱신될 때까지 대기합니다.
     */
    private void selectCleanerFilter() {
        final By LOADER_LOCATOR = By.cssSelector("div.e-loading");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        try {
            WebElement filterLabel = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("label[title='핸디스틱청소기']")));
            filterLabel.click();
            log.info("'핸디스틱청소기' 필터를 클릭했습니다.");

            wait.until(ExpectedConditions.invisibilityOfElementLocated(LOADER_LOCATOR));
            log.info("상품 목록이 성공적으로 갱신되었습니다.");

        } catch (Exception e) {
            log.error("필터 선택 또는 목록 갱신 대기 중 오류 발생", e);
            throw new RuntimeException("필터링에 실패하여 크롤링을 중단합니다.", e);
        }
    }

}

