package kr.co.hdi.crawl.controller;

import jakarta.websocket.server.PathParam;
import kr.co.hdi.crawl.service.EnuriCrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/crawl")
public class CrawlController {

    private final EnuriCrawlerService enuriCrawlerService;

    @PostMapping
    public void crawlProduct(@PathParam("url") String url) {

        enuriCrawlerService.startCrawling(url);
    }
}
