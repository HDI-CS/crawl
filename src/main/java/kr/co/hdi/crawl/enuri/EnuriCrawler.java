package kr.co.hdi.crawl.enuri;

import kr.co.hdi.crawl.AbstractBaseCrawler;
import kr.co.hdi.crawl.domain.Product;
import kr.co.hdi.crawl.domain.ProductImage;
import kr.co.hdi.crawl.repository.ProductImageRepository;
import kr.co.hdi.crawl.repository.ProductRepositoryCustom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public abstract class EnuriCrawler extends AbstractBaseCrawler {

    protected static final String webBaseUrl = "https://www.enuri.com";
    protected static final Pattern MODEL_PATTERN = Pattern.compile("([A-Z0-9-]{5,})");

    protected final ProductRepositoryCustom productRepository;
    protected final ProductImageRepository productImageRepository;

    private static final Random random = new Random();

    // 크롤링 중단 플래그
    private volatile boolean shouldStop = false;

    @Value("${etc.local-image-path:./images}")
    protected String imageStoragePath;

    @Override
    protected void crawl() {
        List<String> allProductUrls = new ArrayList<>();

        int startPage = 23;
        int endPage = 25;

        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║  크롤링 시작 | 카테고리: {} | {}~{}페이지", getCategoryFolderName(), startPage, endPage);
        log.info("╚══════════════════════════════════════════════════════════╝");

        // 시작 페이지로 이동 (1페이지가 아닌 경우)
        if (startPage > 1) {
            if (!goToSpecificPage(startPage)) {
                log.error("❌ 시작 페이지({})로 이동 실패. 크롤링 종료", startPage);
                return;
            }
        }

        for (int currentPage = startPage; currentPage <= endPage; currentPage++) {
            if (shouldStop) {
                log.info("⏹️  크롤링 중단 요청 감지. 페이지 수집 중단");
                break;
            }

            log.info("📄 [페이지 {}/{}] 수집 시작", currentPage, endPage);

            // 현재 페이지에서 상품 URL 수집
            List<String> currentPageUrls = getProductUrl();

            if (currentPageUrls.isEmpty()) {
                log.warn("⚠️  [페이지 {}] 상품 없음. 다음 페이지로 이동", currentPage);
            } else {
                log.info("✅ [페이지 {}] {}개 상품 URL 수집 완료", currentPage, currentPageUrls.size());
                allProductUrls.addAll(currentPageUrls);
            }

            if (currentPage == endPage) {
                log.info("📋 최종 페이지({}) 도달. 페이지 수집 완료", endPage);
                break;
            }

            // 다음 페이지로 이동
            boolean moveSuccess = goToNextPageImproved(currentPage + 1);
            if (!moveSuccess) {
                log.info("⏸️  페이지 {} 이동 실패. 페이지 수집 종료", currentPage + 1);
                break;
            }

            // 페이지 이동 후 잠시 대기
            randomDelay(1000, 2000);
        }

        // 수집된 모든 URL을 순회하며 상세 데이터 수집
        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║  상세 정보 수집 시작 | 총 {}개 상품", String.format("%3d", allProductUrls.size()));
        log.info("╚══════════════════════════════════════════════════════════╝");

        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;

        for (int i = 0; i < allProductUrls.size(); i++) {
            if (shouldStop) {
                log.info("⏹️  크롤링 중단 요청 감지. 상세 정보 수집 중단");
                break;
            }

            String url = allProductUrls.get(i);
            log.info("🔍 [{}/{}] 상품 상세 정보 수집 시작", i + 1, allProductUrls.size());

            try {
                randomDelay(2000, 5000);
                driver.get(webBaseUrl + url);

                Map<String, Object> result = getProductDataWithResult(webBaseUrl + url);
                String status = (String) result.get("status");

                if ("SUCCESS".equals(status)) {
                    successCount++;
                    log.info("   ✅ 저장 완료 | 브랜드: {} | 제품: {}",
                             result.get("company"), result.get("product"));
                } else if ("SKIP".equals(status)) {
                    skipCount++;
                    log.info("   ⏭️  건너뜀 | 사유: {}", result.get("reason"));
                } else {
                    failCount++;
                    log.warn("   ❌ 수집 실패");
                }

            } catch (Exception e) {
                failCount++;
                log.error("   ❌ 오류 발생: {}", e.getMessage());
            }
        }

        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║  크롤링 완료 | 성공: {} | 건너뜀: {} | 실패: {}",
                 String.format("%3d", successCount),
                 String.format("%3d", skipCount),
                 String.format("%3d", failCount));
        log.info("╚══════════════════════════════════════════════════════════╝");
    }

    // 중단 요청 처리 메서드
    public void requestStop() {
        shouldStop = true;
        log.info("🛑 크롤링 중단 요청됨");
    }

    /**
     * 특정 페이지로 직접 이동하는 메서드
     */
    private boolean goToSpecificPage(int targetPage) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // 페이징 컨테이너가 로드될 때까지 대기
            WebElement pagingContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("div.paging")
            ));

            // 현재 표시된 페이지 범위 확인
            List<WebElement> visiblePages = pagingContainer.findElements(By.cssSelector("a.paging__item"));
            int minVisiblePage = Integer.MAX_VALUE;
            int maxVisiblePage = Integer.MIN_VALUE;

            for (WebElement page : visiblePages) {
                try {
                    int pageNum = Integer.parseInt(page.getAttribute("data-page"));
                    minVisiblePage = Math.min(minVisiblePage, pageNum);
                    maxVisiblePage = Math.max(maxVisiblePage, pageNum);
                } catch (NumberFormatException e) {
                    // 페이지 번호를 파싱할 수 없는 경우 무시
                }
            }

            // 목표 페이지가 현재 표시된 범위에 있는지 확인
            if (targetPage >= minVisiblePage && targetPage <= maxVisiblePage) {
                // 직접 페이지 번호 클릭
                return clickPageNumber(targetPage, pagingContainer);
            } else if (targetPage > maxVisiblePage) {
                // 다음 버튼을 통해 페이지 범위 이동
                return navigateToPageRange(targetPage, true, wait);
            } else {
                // 이전 버튼을 통해 페이지 범위 이동
                return navigateToPageRange(targetPage, false, wait);
            }

        } catch (Exception e) {
            log.error("페이지 이동 오류: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 페이지 번호를 직접 클릭하는 메서드
     */
    private boolean clickPageNumber(int targetPage, WebElement pagingContainer) {
        try {
            String pageSelector = String.format("a.paging__item[data-page='%d']", targetPage);
            WebElement pageButton = pagingContainer.findElement(By.cssSelector(pageSelector));

            // 스크롤하여 버튼을 화면에 표시
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", pageButton);
            Thread.sleep(1000);

            // 클릭
            try {
                pageButton.click();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", pageButton);
            }

            // 페이지 변경 확인
            return waitForPageChange(targetPage);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 목표 페이지가 포함된 범위로 이동하는 메서드
     */
    private boolean navigateToPageRange(int targetPage, boolean forward, WebDriverWait wait) {
        int maxAttempts = 10; // 무한루프 방지

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (shouldStop) return false;

            try {
                WebElement pagingContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("div.paging")
                ));

                WebElement navButton;
                if (forward) {
                    navButton = pagingContainer.findElement(By.cssSelector("button.paging__btn--next"));
                    if (navButton.getAttribute("class").contains("is--disabled")) {
                        return false;
                    }
                } else {
                    navButton = pagingContainer.findElement(By.cssSelector("button.paging__btn--prev"));
                    if (navButton.getAttribute("class").contains("is--disabled")) {
                        return false;
                    }
                }

                // 버튼 클릭
                try {
                    navButton.click();
                } catch (Exception e) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", navButton);
                }

                Thread.sleep(2000); // 페이지 로딩 대기

                // 새로운 페이지 범위 확인
                WebElement newPagingContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("div.paging")
                ));

                List<WebElement> newVisiblePages = newPagingContainer.findElements(By.cssSelector("a.paging__item"));
                boolean targetPageVisible = false;

                for (WebElement page : newVisiblePages) {
                    try {
                        int pageNum = Integer.parseInt(page.getAttribute("data-page"));
                        if (pageNum == targetPage) {
                            targetPageVisible = true;
                            break;
                        }
                    } catch (NumberFormatException e) {
                        // 무시
                    }
                }

                if (targetPageVisible) {
                    return clickPageNumber(targetPage, newPagingContainer);
                }

            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }

    /**
     * 개선된 다음 페이지 이동 메서드
     */
    private boolean goToNextPageImproved(int targetPage) {
        if (shouldStop) return false;

        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            WebElement pagingContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("div.paging")
            ));

            // 방법 1: 직접 페이지 번호로 찾기 (가장 안정적)
            try {
                return clickPageNumber(targetPage, pagingContainer);
            } catch (Exception e1) {
                // 방법 2: 다음 버튼 사용
                try {
                    WebElement nextButton = pagingContainer.findElement(By.cssSelector("button.paging__btn--next"));

                    if (nextButton.getAttribute("class").contains("is--disabled")) {
                        return false;
                    }

                    // 스크롤 및 클릭
                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", nextButton);
                    Thread.sleep(1000);

                    try {
                        nextButton.click();
                    } catch (Exception e) {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextButton);
                    }

                    return waitForPageChange(targetPage);

                } catch (Exception e2) {
                    return false;
                }
            }

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 페이지 변경 대기 메서드
     */
    private boolean waitForPageChange(int expectedPage) {
        int maxWaitSeconds = 15;

        for (int i = 0; i < maxWaitSeconds; i++) {
            if (shouldStop) return false;

            try {
                WebElement currentPageElement = driver.findElement(By.cssSelector("a.paging__item.is--on"));
                String currentPageText = currentPageElement.getText();

                if (String.valueOf(expectedPage).equals(currentPageText)) {
                    // 상품 목록이 로드될 때까지 추가 대기
                    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.goods-list")));
                    wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("li.prodItem")));

                    return true;
                }

                Thread.sleep(1000);

            } catch (Exception e) {
                // 계속 시도
            }
        }

        return false;
    }

    private void randomDelay(int minMs, int maxMs) {
        if (shouldStop) return;

        int delay = minMs + random.nextInt(maxMs - minMs + 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected List<String> getProductUrl() {
        List<String> urls = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        try {
            WebElement goodsList = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.goods-list")));
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("li.prodItem")));

            List<WebElement> prodItems = goodsList.findElements(By.cssSelector("li.prodItem"));

            for (WebElement item : prodItems) {
                try {
                    WebElement linkElement = item.findElement(By.cssSelector("div.item__thumb a"));
                    String url = linkElement.getDomAttribute("href");
                    if (url != null && !url.isEmpty()) {
                        urls.add(url);
                    }
                } catch (Exception e) {
                    // 개별 아이템 실패는 무시
                }
            }
        } catch (Exception e) {
            // 상품 목록이 없는 경우
        }

        return urls;
    }

    protected void getProductData(String productUrl) {
        Map<String, String> productInfo = getProductInfo();
        if (productInfo == null || productInfo.isEmpty()) {
            return;
        }

        String productTypeName = getProductTypeName();
        productInfo.put("제품유형", productTypeName);
        List<String> productImages = getProductImage();
        List<String> detailImage = getProductDetailImage();
        saveProduct(productInfo, productImages, detailImage, productUrl);
    }

    // 결과와 함께 반환하는 새로운 메서드
    protected Map<String, Object> getProductDataWithResult(String productUrl) {
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, String> productInfo = getProductInfo();
            if (productInfo == null || productInfo.isEmpty()) {
                result.put("status", "FAIL");
                result.put("reason", "상품 정보 수집 실패");
                return result;
            }

            String companyName = productInfo.get("회사명");
            String productName = productInfo.get("제품명");
            String productPath = getProductPath();

            // 중복 체크
            if (productRepository.existsBySimilarProductName(companyName, productName, productPath)) {
                result.put("status", "SKIP");
                result.put("reason", "DB 중복");
                result.put("company", companyName);
                result.put("product", productName);
                return result;
            }

            List<String> productImages = getProductImage();
            List<String> detailImage = getProductDetailImage();
            saveProduct(productInfo, productImages, detailImage, productUrl);

            result.put("status", "SUCCESS");
            result.put("company", companyName);
            result.put("product", productName);
            result.put("category", productInfo.get("품목"));

        } catch (Exception e) {
            result.put("status", "FAIL");
            result.put("reason", e.getMessage());
        }

        return result;
    }

    @Transactional
    protected void saveProduct(Map<String, String> productInfo, List<String> productImages, List<String> detailImages, String productUrl) {
        String companyName = productInfo.get("회사명");
        String productName = productInfo.get("제품명");
        String productPath = getProductPath();

        if (productRepository.existsBySimilarProductName(companyName, productName, productPath)) {
            return;
        }

        Product product = Product.from(productInfo, productUrl);
        productRepository.save(product);

        String productFolderPath = createProductFolder(product.getId(), product.getProductName());

        List<String> localImagePaths = downloadImagesToLocal(productImages, productFolderPath, "thumbnails");
        List<String> detailLocalImagePaths = downloadImagesToLocal(detailImages, productFolderPath, "details");

        List<ProductImage> images = ProductImage.createThumbnail(product, productImages);
        productImageRepository.saveAll(images);

        List<ProductImage> details = ProductImage.createDetailImage(product, detailImages);
        productImageRepository.saveAll(details);
    }

    protected Map<String, String> getProductInfo() {
        if (shouldStop) return null;

        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            String verificationKeyword = getVerificationKeyword();

            // 키워드가 있을 경우에만 검증 로직 수행
            if (verifyKeyword(verificationKeyword, wait)) return null;

            // 1. 제품 이름
            WebElement prodSum = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.vip-summ__prod")));
            String productName = prodSum.findElement(By.cssSelector("div.vip__tx--title")).getText().trim();

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
            parse(specItems, productName);

            extractProductInfoItems(specItems, wait);

            for (int i = 0; i < dts.size(); i++) {
                WebElement dd = dds.get(i);
                List<WebElement> rows = dd.findElements(By.tagName("tr"));
                for (WebElement row : rows) {
                    List<WebElement> ths = row.findElements(By.tagName("th"));
                    List<WebElement> tds = row.findElements(By.tagName("td"));
                    for (int j = 0; j < ths.size() && j < tds.size(); j++) {
                        String key = ths.get(j).getText().trim();
                        if (key.contains("크기")) {
                            key = "크기";
                        }
                        String value = tds.get(j).getText().trim();
                        specItems.put(key, value);
                    }
                }
            }

            String productPath = getProductPath();
            specItems.put("제품경로", productPath);

            return specItems;

        } catch (Exception e) {
            log.error("상품 정보 수집 실패: {}", e.getMessage());
            return null;
        }
    }

    protected List<String> getProductImage() {
        if (shouldStop) return new ArrayList<>();

        List<String> imageUrls = new ArrayList<>();

        try {
            WebElement thumbList = driver.findElement(By.cssSelector("ul.thum__list"));
            List<WebElement> images = thumbList.findElements(By.tagName("img"));

            for (WebElement img : images) {
                String url = img.getAttribute("src");
                if (url != null && !url.isEmpty() && !url.contains("youtube.com")) {
                    imageUrls.add(url);
                }
            }
        } catch (Exception e) {
            // 이미지를 찾을 수 없는 경우 빈 리스트 반환
        }

        return imageUrls;
    }

    private List<String> getProductDetailImage() {
        if (shouldStop) return new ArrayList<>();

        List<String> imageUrls = new ArrayList<>();

        try {
            // 첫 번째 시도: div.thum_wrap에서 이미지 찾기
            WebElement detailElement = driver.findElement(By.cssSelector("div.thum_wrap"));
            List<WebElement> images = detailElement.findElements(By.tagName("img"));

            for (WebElement image : images) {
                String url = image.getAttribute("src");
                if (url != null && !url.isEmpty() && !url.toLowerCase().endsWith(".gif")) {
                    imageUrls.add(url);
                }
            }
        } catch (Exception e) {
            try {
                // 두 번째 시도: div.tx_wrap.cw__cont에서 p 태그 내 img 찾기
                WebElement txWrapElement = driver.findElement(By.cssSelector("div.tx_wrap.cw__cont"));
                List<WebElement> pTags = txWrapElement.findElements(By.tagName("p"));

                for (WebElement pTag : pTags) {
                    List<WebElement> images = pTag.findElements(By.tagName("img"));
                    for (WebElement image : images) {
                        String url = image.getAttribute("src");
                        if (url != null && !url.isEmpty() && !url.toLowerCase().endsWith(".gif")) {
                            imageUrls.add(url);
                        }
                    }
                }
            } catch (Exception ex) {
                // 상세 이미지를 찾을 수 없는 경우 빈 리스트 반환
            }
        }

        return imageUrls;
    }

    private List<String> downloadImagesToLocal(List<String> imageUrls, String productFolderPath, String subFolderName) {
        if (shouldStop) return new ArrayList<>();

        List<String> localPaths = new ArrayList<>();

        // 하위 폴더 생성 (thumbnails 또는 details)
        String subFolderPath = createSubFolder(productFolderPath, subFolderName);

        for (int i = 0; i < imageUrls.size(); i++) {
            if (shouldStop) break;

            String imageUrl = imageUrls.get(i);
            try {
                String localPath = downloadImage(imageUrl, subFolderPath, i, subFolderName);
                if (localPath != null) {
                    localPaths.add(localPath);
                }
            } catch (Exception e) {
                // 개별 이미지 다운로드 실패는 무시
            }
        }

        return localPaths;
    }

    /**
     * 상품별 메인 폴더 생성
     */
    protected String createProductFolder(Long productId, String productName) {
        String categoryFolder = getCategoryFolderName();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // imageStoragePath / [카테고리명] / [상품명_날짜] 형태로 경로 생성
        String folderPath = Paths.get(imageStoragePath, categoryFolder, productName + "_" + timestamp).toString();

        try {
            Path path = Paths.get(folderPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            log.error("폴더 생성 실패: {}", e.getMessage());
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
            }
        } catch (IOException e) {
            // 폴더 생성 실패 무시
        }

        return subFolderPath;
    }

    /**
     * 개별 이미지 다운로드
     */
    private String downloadImage(String imageUrl, String folderPath, int index, String prefix) throws IOException {
        if (shouldStop) return null;

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

    public static void parse(Map<String, String> map, String name) {
        String cleanName = name.replaceAll("\\[.*?\\]|\\(.*?\\)", "").trim();

        Matcher matcher = MODEL_PATTERN.matcher(cleanName);
        String modelName = "";
        int modelStartIndex = -1;

        while (matcher.find()) {
            modelName = matcher.group(1);
            modelStartIndex = matcher.start();
        }

        String companyName = "";
        String productName = "";

        if (modelStartIndex != -1) {
            String remainingPart = cleanName.substring(0, modelStartIndex).trim();
            String[] parts = remainingPart.split("\\s+");

            if (parts.length > 1) {
                companyName = parts[0];
                productName = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
            } else {
                productName = remainingPart;
            }
        } else {
            // 모델명을 찾지 못한 경우, 전체를 제품명으로
            productName = cleanName;
        }

        map.put("회사명", companyName);
        map.put("제품명", productName);
        map.put("모델명", modelName);
    }

    protected abstract String getCategoryFolderName();

    /**
     * 상품 정보를 확인할때 사용합니다. 자식 클래스가 선택적으로 재정의할 수 있는 검증 키워드를 반환.
     * 기본적으로는 null을 반환하여 검증을 수행하지 않도록 합니다.
     * @return 검증에 사용할 키워드, 없으면 null
     */
    protected String getVerificationKeyword() {
        return null;
    }

    protected abstract String getProductPath();

    private boolean verifyKeyword(String verificationKeyword, WebDriverWait wait) {
        if (verificationKeyword != null && !verificationKeyword.isEmpty()) {
            try {
                // 제품명 먼저 가져오기
                String productName = "";
                try {
                    WebElement prodSum = wait.until(ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector("div.vip-summ__prod")));
                    productName = prodSum.findElement(By.cssSelector("div.vip__tx--title")).getText().trim();
                } catch (Exception e) {
                    productName = "제품명 확인 불가";
                }

                // 1. 먼저 부모 컨테이너가 로드될 때까지 기다림
                WebElement infoContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("div.vip-summ__info")
                ));

                // 2. JavaScript로 스크롤하여 요소가 viewport에 보이도록 함
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", infoContainer);

                // 3. 잠시 대기 후 다시 시도
                Thread.sleep(1000);

                // 4. 여러 방법으로 요소 찾기 시도
                List<WebElement> infoItems = null;

                // 방법 1: 원래 방식
                try {
                    infoItems = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                            By.cssSelector("span.vip-summ__info-item")
                    ));
                } catch (TimeoutException e1) {
                    // 방법 2: 부모에서 직접 찾기
                    try {
                        infoItems = infoContainer.findElements(By.cssSelector("span.vip-summ__info-item"));
                        if (infoItems.isEmpty()) {
                            // 방법 3: XPath 사용
                            infoItems = driver.findElements(By.xpath("//span[@class='vip-summ__info-item']"));
                            if (infoItems.isEmpty()) {
                                // 방법 4: JavaScript로 직접 찾기
                                String script = "return document.querySelectorAll('span.vip-summ__info-item');";
                                List<WebElement> jsElements = (List<WebElement>) ((JavascriptExecutor) driver).executeScript(script);
                                if (jsElements != null && !jsElements.isEmpty()) {
                                    infoItems = jsElements;
                                }
                            }
                        }
                    } catch (Exception e2) {
                        return false;
                    }
                }

                if (infoItems == null || infoItems.isEmpty()) {
                    return false;
                }

                boolean isVerified = false;
                String actualCategory = "";

                for (WebElement item : infoItems) {
                    try {
                        String text = item.getText();

                        if (text != null && text.startsWith("품목")) {
                            // 실제 품목 추출
                            if (text.contains(" : ")) {
                                String[] parts = text.split(" : ", 2);
                                if (parts.length == 2) {
                                    actualCategory = parts[1].trim();
                                }
                            }

                            if (text.contains(verificationKeyword)) {
                                isVerified = true;
                                log.info("   📦 품목 확인: {} | 제품: {}", verificationKeyword, productName);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }

                if (!isVerified) {
                    log.info("   ⏭️  품목 불일치: {} 아님 (실제: {}) | 제품: {} | 건너뜀",
                             verificationKeyword,
                             actualCategory.isEmpty() ? "확인불가" : actualCategory,
                             productName);
                    return true;
                }

            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private void extractProductInfoItems(Map<String, String> specItems, WebDriverWait wait) {
        try {
            // info-item 요소들 찾기
            List<WebElement> infoItems = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                    By.cssSelector("span.vip-summ__info-item")
            ));

            for (WebElement item : infoItems) {
                try {
                    String text = item.getText().trim();

                    if (text.contains(" : ")) {
                        String[] parts = text.split(" : ", 2);
                        if (parts.length == 2) {
                            String key = parts[0].trim();
                            String value = parts[1].trim();

                            // 특별히 추출하고 싶은 정보들
                            switch (key) {
                                case "등록일":
                                    specItems.put("등록일", value);
                                    break;
                                case "제조사":
                                    specItems.put("제조사", value);
                                    break;
                                case "브랜드":
                                    specItems.put("브랜드", value);
                                    break;
                                case "출시가":
                                    specItems.put("출시가", value);
                                    break;
                                case "품목":
                                    specItems.put("품목", value);
                                    break;
                                default:
                                    // 다른 모든 정보도 저장하고 싶다면 아래 주석 해제
                                    // specItems.put(key, value);
                                    break;
                            }
                        }
                    }
                } catch (Exception e) {
                    // 개별 item 처리 실패는 무시
                }
            }
        } catch (Exception e) {
            // 상품 정보 항목 추출 실패는 무시하고 계속 진행
        }
    }

    protected abstract String getProductTypeName();
}