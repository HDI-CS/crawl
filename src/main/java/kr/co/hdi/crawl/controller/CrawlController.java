package kr.co.hdi.crawl.controller;

import jakarta.websocket.server.PathParam;
import kr.co.hdi.crawl.kakao.KakaoCrawlerService;
import kr.co.hdi.crawl.kakao.KakaoLogoService;
import kr.co.hdi.crawl.pandarank.PandarankCrawlerService;
import kr.co.hdi.crawl.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/crawl")
public class CrawlController {

    private final EnuriCrawlerService enuriCrawlerService;
    private final OliveCrawlerService oliveCrawlerService;
    private final KakaoCrawlerService kakaoCrawlerService;
    private final KakaoLogoService kakaoLogoService;
    private final PandarankCrawlerService pandarankCrawlerService;

    @PostMapping
    public void crawlProduct(@PathParam("url") String url) {

        enuriCrawlerService.startCrawling(url);
    }

    @PostMapping("/brand")
    public void getBrandNameInOliveYoung(@PathParam("url") String url) {

        oliveCrawlerService.startCrawling(url);
    }

    @PostMapping("/pandarank")
    public void getPandarankData(@PathParam("url") String url) {

        pandarankCrawlerService.startCrawling(url);
    }

    @PostMapping("/kakao")
    public void getBrandList(@PathParam("url") String url) {

        kakaoCrawlerService.startCrawling(url);
    }

    @PostMapping("/kakao/logo")
    public void getKakaoLogo() {

        kakaoLogoService.getLogoImage();
    }
}
