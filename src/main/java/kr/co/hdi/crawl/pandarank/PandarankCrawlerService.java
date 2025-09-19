package kr.co.hdi.crawl.pandarank;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import kr.co.hdi.crawl.AbstractBaseCrawler;
import kr.co.hdi.crawl.dto.CrawlTarget;
import kr.co.hdi.crawl.dto.PandarankItemInfoDto;
import kr.co.hdi.crawl.dto.ProductType;
import kr.co.hdi.crawl.dto.SiteType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.FileWriter;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PandarankCrawlerService extends AbstractBaseCrawler {

    @Value("${storage.panda-rank.input-csv}")
    private String inputCsvPath;

    @Value("${storage.panda-rank.output-csv}")
    private String outputCsvPath;

    private static final String PANDA_RANK_BASE_URL = "https://pandarank.net/search/detail?keyword=";

    @Override
    protected void crawl() {

        List<PandarankItemInfoDto> results = new ArrayList<>();

        login();
        List<String> brandNames = getBrandName();

        for (int i = 0; i < 300; i++) {

            String name = brandNames.get(i);
            String searchUrl = getSearchUrl(name);
            PandarankItemInfoDto result = getData(searchUrl, name);
            results.add(result);

            // 6개 간격마다 1분 쉬기
            if (i > 0 && i % 6 == 0) {
                try {
                    log.info("6개 처리 완료. 1분간 대기합니다");
                    Thread.sleep(60 * 1000);
                } catch (InterruptedException e) {
                    log.error("스레드 대기 중 인터럽트 발생", e);
                }
            }
        }
        savePandaRankDataToCsv(results, outputCsvPath);
    }

    private List<String> getBrandName() {

        List<String> brandNames = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(inputCsvPath))) {
            reader.readNext();  // 처음은 헤더라서 스킵
            String[] line;
            while ((line = reader.readNext()) != null && brandNames.size() != 602) {  // 여기 사이즈 잘 나눠주기

                String name = line[0].trim();
                brandNames.add(name);
            }
        } catch (Exception e) {
            log.error("csv 파일 읽는 중 에러 발생", e);
        }
        return brandNames;
    }

    private void login() {

        WebElement loginLink = driver.findElement(By.cssSelector("a[href='/user/login']"));
        loginLink.click();

        // 네이버 로그인
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement naverLoginBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[contains(@class, 'login-button')]//span[text()='네이버로 시작하기']")
        ));
        naverLoginBtn.click();

        // 아이디
        WebElement idInput = wait.until(driver -> driver.findElement(By.id("id")));
        idInput.clear();
        idInput.sendKeys("seoji0538");

        // 비밀번호
        WebElement pwInput = wait.until(driver -> driver.findElement(By.id("pw")));
        pwInput.clear();
        pwInput.sendKeys("test password");

        WebElement loginBtn = wait.until(driver -> driver.findElement(By.id("log.login")));
        loginBtn.click();

        // 직접 네이버 캐챠 입력하기
        WebDriverWait loginWait = new WebDriverWait(driver, Duration.ofMinutes(2));
        loginWait.until(d -> {
            try {
                WebElement myElement = d.findElement(By.cssSelector("div.relative"));
                return myElement.isDisplayed();
            } catch (NoSuchElementException e) {
                return false;
            }
        });
    }

    public PandarankItemInfoDto getData(String url, String name) {

        driver.get(url);

        closePopupIfPresent();

        String category = getCategory();
        String target = getTarget();
        List<String> topItem = getTopItem();

        String first = topItem.size() > 0 ? topItem.get(0) : "N/A";
        String second = topItem.size() > 1 ? topItem.get(1) : "N/A";
        String third = topItem.size() > 2 ? topItem.get(2) : "N/A";

        return new PandarankItemInfoDto(name, category, target, first, second, third);
    }

    public String getCategory() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement divElement = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.name.p-text-compact-sm.p-fg-subtle")
        ));

        String innerHtml = divElement.getAttribute("innerHTML");
        int spanIndex = innerHtml.indexOf("<span");
        if (spanIndex != -1) {
            innerHtml = innerHtml.substring(0, spanIndex);
        }

        String[] parts = innerHtml.split("<i.*?>");
        String category = "";
        for (String part : parts) {
            String text = part.replaceAll("</i>", ">").trim();
            if (!text.isEmpty()) {
                category += text;
            }
        }
        return category;
    }

    public String  getTarget() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        try {
            // bar-chart-data가 뜰 때까지 기다리기
            WebElement targetData = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("div.bar-chart-data")
            ));

            // 화면이 갱신된 이후에 no-data가 보이는지 체크
            wait.until(driver -> {
                boolean hasNoData = !targetData.findElements(By.cssSelector("div.no-data")).isEmpty();
                boolean hasAgeData = !targetData.findElements(By.cssSelector("ul.age-chart li")).isEmpty();
                return hasNoData || hasAgeData; // 둘 중 하나가 뜰 때까지 대기
            });

            // 최종적으로 no-data 메시지가 있는 경우
            List<WebElement> noDataElements = targetData.findElements(By.cssSelector("div.no-data"));
            if (!noDataElements.isEmpty() && noDataElements.get(0).isDisplayed()) {
                return null;
            }

            // 실제 데이터 파싱
            String html = targetData.getAttribute("outerHTML");
            Document doc = Jsoup.parse(html);
            String gender = getGender(doc);
            if (gender == null)
                return null;

            return getGender(doc) + getAge(doc);

        } catch (TimeoutException e) {
            log.warn("차트 데이터 로딩 실패 (Timeout)");
            return null;
        }
    }

    public String getGender(Document doc) {

        Element gender = doc.selectFirst("div.bar-chart.gender");
        Element femaleBar = gender.selectFirst(".female");
        Element maleBar = gender.selectFirst(".male");

        if (femaleBar == null || maleBar == null)
            return null;

        double femalePercent = 0;
        double malePercent = 0;

        String feStyle = femaleBar.attr("style");
        femalePercent = parsePercent(feStyle);

        String maStyle = maleBar.attr("style");
        malePercent = parsePercent(maStyle);

        return  (femalePercent > malePercent) ? "여성/" : "남성/";
    }

    public String getAge(Document doc) {

        List<Map.Entry<Integer, Double>> ageHeights = new ArrayList<>();
        Elements ageElements = doc.select("ul.age-chart li");

        for (Element ageElement : ageElements) {
            String className = ageElement.className();
            int ageNum = Integer.parseInt(className.split(" ")[0].replace("age-", ""));

            Element span = ageElement.selectFirst("span.p-text-compact-xs.p-fg-subtlest");
            double value = 0;

            if (span != null) {
                String spanText = span.text().replace("%", "").trim();
                String style = span.attr("style");

                // 값이 있고, display:none 상태가 아닐 때만 사용
                if (!spanText.equals("-") && !spanText.isEmpty() && !style.contains("display: none")) {
                    try {
                        value = Double.parseDouble(spanText);
                    } catch (NumberFormatException ignored) {}
                }
            }
            ageHeights.add(new AbstractMap.SimpleEntry<>(ageNum, value));
        }
        ageHeights.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        int age1 = ageHeights.get(0).getKey();
        int age2 = ageHeights.get(1).getKey();

        if (age1 > age2) {
            int temp = age1;
            age1 = age2;
            age2 = temp;
        }

        return (age2 - age1 == 10) ? (age1 + "-" + age2 + "대") : (age1 + "대, " + age2 + "대");
    }

    public static double parsePercent(String style) {

        if (style == null || style.isEmpty())
            return 0;
        style = style.replaceAll("[^0-9.]", "");
        if (style.isEmpty()) return 0;
        return Double.parseDouble(style);
    }

    public List<String> getTopItem() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        List<String> topItems = new ArrayList<>();

        try {
            // 상위 3개 상품 가져오기
            List<WebElement> itemElements = wait.until(driver ->
                    driver.findElements(By.cssSelector("div.item-prod"))
            );

            for (int i = 0; i < Math.min(3, itemElements.size()); i++) {
                WebElement item = itemElements.get(i);
                WebElement nameElement = item.findElement(By.cssSelector("div.prod-title"));
                String name = nameElement.getAttribute("textContent").trim();

                // 정규식으로 불필요한 부분 제거
                name = name.replaceAll("\\(.*?\\)", "");                          // 괄호 제거
                name = name.replaceAll("\\[.*?\\]", "");                          // 대괄호 제거
                name = name.replaceAll("\\d+(\\.\\d+)?\\s*(ml|g|kg|L|l)", "");    // 용량 제거
                name = name.replaceAll("\\d+개", "");                             // 수량 제거
                name = name.replaceAll("[+,]", "");                               // +, 제거
                name = name.replaceAll("\\s+", " ").trim();                       // 공백 정리

                topItems.add(name);
            }
        } catch (TimeoutException e) {
            topItems.add("N/A");
        }
        return topItems;
    }

    // 팝업 뜨면 닫기 (무료 회원 팝업)
    public void closePopupIfPresent() {

        By closeBtnSelector = By.id("btn-first");
        List<WebElement> closeButtons = driver.findElements(closeBtnSelector);

        if (!closeButtons.isEmpty() && closeButtons.get(0).isDisplayed()) {
            closeButtons.get(0).click();
            log.info("팝업 닫기 버튼 클릭 완료");
        }

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
        try {

            // 모달 안 확인 버튼이 나타날 때까지 대기
            WebElement confirmBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(By.id("new-btn-cancel"))
            );
            confirmBtn.click();
        } catch (Exception e) {}
    }

    public String getSearchUrl(String key) {

        try {

            String encodedBrand = java.net.URLEncoder.encode(key, "UTF-8");
            return (PANDA_RANK_BASE_URL + encodedBrand);
        } catch (Exception e) {
            log.error("url 인코딩 중 에러 발생", e);
        }
        return null;
    }

    public void savePandaRankDataToCsv(List<PandarankItemInfoDto> infoDtos, String filePath) {

        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {

            // 헤더 작성
            String[] header = {"브랜드명", "대표 제품 카테고리", "타겟(성별/연령)", "대표 제품1", "대표 제품2", "대표 제품3"};
            writer.writeNext(header);

            // 데이터 작성
            for (PandarankItemInfoDto dto : infoDtos) {
                String[] line = {
                        dto.brandName(),
                        dto.category(),
                        dto.target(),
                        dto.topItem(),
                        dto.item2(),
                        dto.item3()
                };
                writer.writeNext(line);
            }

        } catch (Exception e) {
            log.error("panda rank 브랜드 데이터 csv 출력 중 에러 발생", e);
        }
    }

    @Override
    public boolean supports(CrawlTarget target) {
        return target.siteType() == SiteType.PANDARANK && target.productType() == ProductType.BRAND;
    }
}
