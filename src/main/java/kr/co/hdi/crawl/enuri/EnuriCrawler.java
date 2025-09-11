package kr.co.hdi.crawl.enuri;

import kr.co.hdi.crawl.AbstractBaseCrawler;
import kr.co.hdi.crawl.domain.Product;
import kr.co.hdi.crawl.domain.ProductImage;
import kr.co.hdi.crawl.repository.ProductImageRepository;
import kr.co.hdi.crawl.repository.ProductRepositoryCustom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
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


    @Value("${etc.local-image-path:./images}")
    protected String imageStoragePath;


    @Override
    protected void crawl() {
        List<String> allProductUrls = new ArrayList<>();
        final int MAX_PAGES_TO_CRAWL = 50;

        for (int currentPage = 1; currentPage <= MAX_PAGES_TO_CRAWL; currentPage++) {
            log.info("===== 현재 페이지: {} 수집 시작 =====", currentPage);

            // 현재 페이지에서 상품 URL 수집
            List<String> currentPageUrls = getProductUrl();
            log.info("페이지 {}에서 {}개의 상품 URL 수집", currentPage, currentPageUrls.size());
            allProductUrls.addAll(currentPageUrls);

            if (currentPage == MAX_PAGES_TO_CRAWL) {
                log.info("최대 {}페이지까지 수집을 완료했습니다.", MAX_PAGES_TO_CRAWL);
                break;
            }

            // 다음 페이지로 이동
            if (!goToNextPage(currentPage + 1)) {
                log.info("다음 페이지({})로 이동할 수 없어 수집을 종료합니다.", currentPage + 1);
                break;
            }
        }

        // 수집된 모든 URL을 순회하며 상세 데이터 수집
        log.info("총 {}개의 상품 URL을 수집했습니다. 상세 정보 수집을 시작합니다.", allProductUrls.size());
        for (String url : allProductUrls) {
            try {
                randomDelay(2000, 5000);

                driver.get(webBaseUrl + url);
                getProductData(webBaseUrl + url);
            } catch (Exception e) {
                log.error("{} 페이지 상세 정보 수집 중 오류 발생", url, e);
            }
        }
    }

    private void randomDelay(int minMs, int maxMs) {
        int delay = minMs + random.nextInt(maxMs - minMs + 1);
        try {
            Thread.sleep(delay);
            log.debug("랜덤 지연: {}ms", delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected List<String> getProductUrl() {

        List<String> urls = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

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
                    log.warn("개별 상품 아이템에서 URL을 추출하는 데 실패했습니다.");
                }
            }
        } catch (Exception e) {
            log.warn("상품 목록을 찾을 수 없거나 페이지에 상품이 없습니다. URL: {}", driver.getCurrentUrl());
        }

        return urls;
    }

    protected void getProductData(String productUrl) {
        Map<String, String> productInfo = getProductInfo();
        List<String> productImages = getProductImage();
        List<String> detailImage = getProductDetailImage();
        saveProduct(productInfo, productImages, detailImage, productUrl);
    }


    @Transactional
    protected void saveProduct(Map<String, String> productInfo, List<String> productImages, List<String> detailImages, String productUrl) {

        String companyName = productInfo.get("회사명");
        String productName = productInfo.get("제품명");

        String productPath = getProductPath();

        if (productRepository.existsBySimilarProductName(companyName, productName, productPath)) {
            log.info("[저장 건너뜀] (DB 중복) 브랜드: {}, 제품명: {}", companyName, productName);
            return;
        }

        Product product = Product.from(productInfo, productUrl);
        productRepository.save(product);

        String productFolderPath = createProductFolder(product.getId(), product.getProductName());

        List<String> localImagePaths = downloadImagesToLocal(productImages, productFolderPath, "thumbnails");
        log.info("상품 이미지 경로: {}", localImagePaths);

        List<String> detailLocalImagePaths = downloadImagesToLocal(detailImages, productFolderPath, "details");
        log.info("상세 이미지 경로: {}", detailLocalImagePaths);

        List<ProductImage> images = ProductImage.createThumbnail(product, productImages);
        productImageRepository.saveAll(images);

        List<ProductImage> details = ProductImage.createDetailImage(product, detailImages);
        productImageRepository.saveAll(details);
    }

    protected Map<String, String> getProductInfo() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        String verificationKeyword = getVerificationKeyword(); // 자식에게 키워드를 물어봄

        // 키워드가 있을 경우에만 검증 로직 수행
        if (verifyKeyword(verificationKeyword, wait)) return null;


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
        specItems.forEach((k, v) -> log.info("{} : {}", k, v));

        String productPath = getProductPath();
        specItems.put("제품경로", productPath);

        return specItems;
    }

    protected List<String> getProductImage() {
        List<String> imageUrls = new ArrayList<>();

        WebElement thumbList = driver.findElement(By.cssSelector("ul.thum__list"));
        List<WebElement> images = thumbList.findElements(By.tagName("img"));

        for (WebElement img : images) {
            String url = img.getAttribute("src");
            if (url != null && !url.isEmpty() && !url.contains("youtube.com")) {
                imageUrls.add(url);
//                log.info("이미지 URL: {}", url);
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
                if (url != null && !url.isEmpty() && !url.toLowerCase().endsWith(".gif")) {
                    imageUrls.add(url);
//                    log.info("상세 이미지 URL (thum_wrap): {}", url);
                } else if (url != null && url.toLowerCase().endsWith(".gif")) {
                    log.debug("GIF 파일 제외됨: {}", url);
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
                        if (url != null && !url.isEmpty() && !url.toLowerCase().endsWith(".gif")) {
                            imageUrls.add(url);
//                            log.info("상세 이미지 URL (thum_wrap): {}", url);
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
    protected String createProductFolder(Long productId, String productName) {
        String categoryFolder = getCategoryFolderName();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // imageStoragePath / [카테고리명] / [상품명_날짜] 형태로 경로 생성
        String folderPath = Paths.get(imageStoragePath, categoryFolder, productName + "_" + timestamp).toString();

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
                    log.info("방법 1 성공: {}개의 info-item 발견", infoItems.size());
                } catch (TimeoutException e1) {
                    log.warn("방법 1 실패, 방법 2 시도");

                    // 방법 2: 부모에서 직접 찾기
                    try {
                        infoItems = infoContainer.findElements(By.cssSelector("span.vip-summ__info-item"));
                        if (infoItems.isEmpty()) {
                            log.warn("방법 2 실패, 방법 3 시도");

                            // 방법 3: XPath 사용
                            infoItems = driver.findElements(By.xpath("//span[@class='vip-summ__info-item']"));
                            if (infoItems.isEmpty()) {
                                log.warn("방법 3도 실패, 방법 4 시도");

                                // 방법 4: JavaScript로 직접 찾기
                                String script = "return document.querySelectorAll('span.vip-summ__info-item');";
                                List<WebElement> jsElements = (List<WebElement>) ((JavascriptExecutor) driver).executeScript(script);
                                if (jsElements != null && !jsElements.isEmpty()) {
                                    infoItems = jsElements;
                                    log.info("방법 4 성공: JavaScript로 {}개 요소 발견", infoItems.size());
                                }
                            } else {
                                log.info("방법 3 성공: XPath로 {}개 요소 발견", infoItems.size());
                            }
                        } else {
                            log.info("방법 2 성공: 부모에서 {}개 요소 발견", infoItems.size());
                        }
                    } catch (Exception e2) {
                        log.error("모든 방법 실패: {}", e2.getMessage());
                        return false;
                    }
                }

                if (infoItems == null || infoItems.isEmpty()) {
                    log.error("어떤 방법으로도 vip-summ__info-item 요소를 찾을 수 없습니다.");

                    // 디버그를 위해 페이지 소스 일부 출력
                    String pageSource = driver.getPageSource();
                    if (pageSource.contains("vip-summ__info-item")) {
                        log.debug("페이지 소스에는 vip-summ__info-item이 존재합니다.");
                        log.debug("현재 URL: {}", driver.getCurrentUrl());
                    }

                    return false;
                }

                boolean isVerified = false;

                for (WebElement item : infoItems) {
                    try {
                        String text = item.getText();
                        log.debug("검사 중인 텍스트: {}", text);

                        if (text != null && text.startsWith("품목")) {
                            if (text.contains(verificationKeyword)) {
                                isVerified = true;
                                log.info("페이지 검증 성공: '{}' 상품이 맞습니다.", verificationKeyword);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        log.warn("개별 요소 처리 중 오류: {}", e.getMessage());
                        continue;
                    }
                }

                if (!isVerified) {
                    log.warn("검증 실패: 이 상품은 '{}'이(가) 아닙니다. 수집을 건너뜁니다.", verificationKeyword);
                    return true;
                }

            } catch (Exception e) {
                log.error("키워드 검증 중 예외 발생: {}", e.getMessage(), e);
                return false;
            }
        }
        return false;
    }

    /**
     * 다음 페이지로 이동하는 메서드
     */
    private boolean goToNextPage(int targetPage) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            // 1. 페이징 컨테이너가 로드될 때까지 대기
            WebElement pagingContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("div.paging")
            ));

            // 2. 현재 페이지 정보 확인 (디버깅용)
            try {
                WebElement currentPageElement = pagingContainer.findElement(By.cssSelector("a.paging__item.is--on"));
                String currentPageText = currentPageElement.getText();
                log.info("현재 활성 페이지: {}", currentPageText);
            } catch (Exception e) {
                log.warn("현재 페이지 정보를 확인할 수 없습니다: {}", e.getMessage());
            }

            // 3. 페이지 변경 전 현재 상태 저장
            String currentUrl = driver.getCurrentUrl();
            WebElement referenceElement = driver.findElement(By.cssSelector("div.goods-list"));

            // 4. 다양한 방법으로 다음 페이지 버튼 찾기 시도
            WebElement nextButton = null;

            // 방법 1: 정확한 페이지 번호로 찾기
            try {
                String nextButtonSelector = String.format("a.paging__item[data-page='%d']", targetPage);
                nextButton = pagingContainer.findElement(By.cssSelector(nextButtonSelector));
                log.info("방법 1 성공: 페이지 {} 버튼 발견", targetPage);
            } catch (Exception e1) {
                log.warn("방법 1 실패, 방법 2 시도");

                // 방법 2: 다음 버튼(Next) 사용
                try {
                    nextButton = pagingContainer.findElement(By.cssSelector("button.paging__btn--next"));
                    if (nextButton.getAttribute("class").contains("is--disabled")) {
                        log.info("다음 버튼이 비활성화되어 있습니다. 마지막 페이지입니다.");
                        return false;
                    }
                    log.info("방법 2 성공: 다음 버튼 사용");
                } catch (Exception e2) {
                    log.warn("방법 2 실패, 방법 3 시도");

                    // 방법 3: XPath로 찾기
                    try {
                        String xpath = String.format("//a[@class='paging__item' and @data-page='%d']", targetPage);
                        nextButton = driver.findElement(By.xpath(xpath));
                        log.info("방법 3 성공: XPath로 페이지 {} 버튼 발견", targetPage);
                    } catch (Exception e3) {
                        log.error("모든 방법으로 다음 페이지 버튼을 찾을 수 없습니다.");

                        // 디버그 정보 출력
                        List<WebElement> allPagingItems = pagingContainer.findElements(By.cssSelector("a.paging__item"));
                        log.debug("사용 가능한 페이징 버튼들:");
                        for (WebElement item : allPagingItems) {
                            String page = item.getAttribute("data-page");
                            String classes = item.getAttribute("class");
                            log.debug("  - 페이지: {}, 클래스: {}", page, classes);
                        }
                        return false;
                    }
                }
            }

            if (nextButton == null) {
                return false;
            }

            // 5. 버튼이 클릭 가능한 상태인지 확인
            wait.until(ExpectedConditions.elementToBeClickable(nextButton));

            // 6. 스크롤하여 버튼을 화면에 표시
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", nextButton);
            Thread.sleep(1000); // 스크롤 완료 대기

            // 7. 클릭 시도 (여러 방법)
            boolean clickSuccess = false;

            // 방법 1: 일반 클릭
            try {
                nextButton.click();
                clickSuccess = true;
                log.info("방법 1로 버튼 클릭 성공");
            } catch (Exception e1) {
                log.warn("방법 1 클릭 실패, JavaScript 클릭 시도");

                // 방법 2: JavaScript 클릭
                try {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextButton);
                    clickSuccess = true;
                    log.info("방법 2로 JavaScript 클릭 성공");
                } catch (Exception e2) {
                    log.error("모든 클릭 방법 실패: {}", e2.getMessage());
                    return false;
                }
            }

            if (!clickSuccess) {
                return false;
            }

            log.info("다음 페이지({})로 이동 시도 완료", targetPage);

            // 8. 페이지 변경 확인 (여러 조건으로 확인)
            boolean pageChanged = false;
            int maxAttempts = 10;

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    // 조건 1: 기존 요소가 stale 상태가 되었는지 확인
                    try {
                        referenceElement.isDisplayed();
                    } catch (StaleElementReferenceException e) {
                        pageChanged = true;
                        log.info("시도 {}번째: 기존 요소가 stale 상태가 됨 (페이지 변경됨)", attempt);
                        break;
                    }

                    // 조건 2: URL 변경 확인
                    String newUrl = driver.getCurrentUrl();
                    if (!newUrl.equals(currentUrl)) {
                        pageChanged = true;
                        log.info("시도 {}번째: URL 변경 감지 ({})", attempt, newUrl);
                        break;
                    }

                    // 조건 3: 활성 페이지 번호 변경 확인
                    try {
                        WebElement newCurrentPage = driver.findElement(By.cssSelector("a.paging__item.is--on"));
                        String newPageNumber = newCurrentPage.getText();
                        if (String.valueOf(targetPage).equals(newPageNumber)) {
                            pageChanged = true;
                            log.info("시도 {}번째: 활성 페이지 번호가 {}로 변경됨", attempt, newPageNumber);
                            break;
                        }
                    } catch (Exception e) {
                        // 활성 페이지 요소를 찾을 수 없음
                    }

                    Thread.sleep(1000); // 1초 대기 후 다시 확인

                } catch (Exception e) {
                    log.warn("페이지 변경 확인 중 오류 (시도 {}번째): {}", attempt, e.getMessage());
                }
            }

            if (!pageChanged) {
                log.warn("페이지 변경이 감지되지 않았습니다. 다음 페이지로 이동하지 못했을 수 있습니다.");
                return false;
            }

            // 9. 새 페이지의 상품 목록이 로드될 때까지 대기
            try {
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.goods-list")));
                wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("li.prodItem")));
                log.info("새 페이지의 상품 목록 로드 완료");
            } catch (Exception e) {
                log.warn("새 페이지의 상품 목록 로드 확인 실패: {}", e.getMessage());
                // 실패해도 계속 진행
            }

            return true;

        } catch (Exception e) {
            log.error("페이지 이동 중 예외 발생: {}", e.getMessage(), e);
            return false;
        }
    }


    private void extractProductInfoItems(Map<String, String> specItems, WebDriverWait wait) {
        try {
            // info-item 요소들 찾기
            List<WebElement> infoItems = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                    By.cssSelector("span.vip-summ__info-item")
            ));

            log.info("{}개의 상품 정보 항목을 발견했습니다.", infoItems.size());

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
                    log.warn("개별 info-item 처리 중 오류: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("상품 정보 항목 추출 중 오류 발생: {}", e.getMessage());
            // 오류가 발생해도 계속 진행
        }
    }

}

