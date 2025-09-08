package kr.co.hdi.crawl;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public abstract class AbstractBaseCrawler implements Crawler{

    protected WebDriver driver;

    protected void initDriver() {

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

    public void start(String webUrl) {
        try {
            initDriver();
            driver.get(webUrl);

            applyCustomLogic();
            crawl();
        } finally {
            driver.quit();
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
    }

}
