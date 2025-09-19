package kr.co.hdi.crawl;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public abstract class AbstractBaseCrawler implements Crawler {

    protected WebDriver driver;

    private static final List<String> USER_AGENTS = Arrays.asList(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    );

    // 다양한 화면 해상도
    private static final List<String> WINDOW_SIZES = Arrays.asList(
            "1920,1080", "1366,768", "1536,864", "1440,900", "1024,768"
    );

    protected void initDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        Random random = new Random();

        // 랜덤 user agent 설정
        String userAgent = USER_AGENTS.get(random.nextInt(USER_AGENTS.size()));
        options.addArguments("--user-agent=" + userAgent);

        // 랜덤 창 크기 설정
        String windowSize = WINDOW_SIZES.get(random.nextInt(WINDOW_SIZES.size()));
        options.addArguments("--window-size=" + windowSize);

        // 봇 탐지 회피를 위한 고급 옵션들
        options.addArguments("--headless"); // 필요에 따라 주석 처리
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--lang=ko_KR");

        // 자동화 탐지 회피
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-notifications");
      
        options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        // 추가 브라우저 플래그들
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-plugins");

        // 메모리 사용량 최적화
        options.addArguments("--memory-pressure-off");
        options.addArguments("--max_old_space_size=4096");

        // 네트워크 관련
        options.addArguments("--aggressive-cache-discard");
        options.addArguments("--disable-background-timer-throttling");
        options.addArguments("--disable-renderer-backgrounding");

        driver = new ChromeDriver(options);

        // 자동화 감지 스크립트 제거
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"
            );
        } catch (Exception e) {
            // JavaScript 실행 실패는 무시
        }
    }

    /**
     * 드라이버 재초기화 (차단 감지 시 사용)
     */
    protected void reinitDriver() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                // 종료 중 에러 무시
            }
        }

        // 재초기화 전 잠시 대기
        try {
            Thread.sleep(2000 + new Random().nextInt(3000)); // 2-5초 랜덤 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        initDriver();
    }

    public void start(String webUrl) {
        try {
            initDriver();
            driver.get(webUrl);

            applyCustomLogic();
            crawl();
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    // 종료 중 에러 무시
                }
            }
        }
    }

    /**
     * start 메서드에서 호출되어 실제 로직을 수행하는 추상 메서드
     * 사이트마다 구체적인 로직을 구현합니다
     */
    protected abstract void crawl();

    /**
     * 자식 클래스가 필요에 따라 재정의할 수 있는 Hook 메서드
     * crawl() 메서드가 실행되기 전에 호출됩니다
     */
    protected void applyCustomLogic() {
        // 기본적으로 빈 구현
    }

    /**
     * 안전한 드라이버 종료
     */
    protected void safeQuitDriver() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                // 에러 무시하고 계속 진행
            } finally {
                driver = null;
            }
        }
    }
}