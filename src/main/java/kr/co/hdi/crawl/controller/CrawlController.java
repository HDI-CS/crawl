package kr.co.hdi.crawl.controller;

import kr.co.hdi.crawl.dto.ProductType;
import kr.co.hdi.crawl.dto.SiteType;
import kr.co.hdi.crawl.dto.request.CrawlRequest;
import kr.co.hdi.crawl.service.CrawlService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/crawl")
public class CrawlController {

    private final CrawlService crawlService;

    @PostMapping
    public void crawlProduct(
            @RequestParam("site") SiteType siteType,
            @RequestParam("product") ProductType productType,
            @RequestBody CrawlRequest request
    ) {
        crawlService.startCrawlingAndSave(siteType, productType, request.url());
    }

}
