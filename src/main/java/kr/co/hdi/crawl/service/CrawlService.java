package kr.co.hdi.crawl.service;

import kr.co.hdi.crawl.Crawler;
import kr.co.hdi.crawl.dto.CrawlTarget;
import kr.co.hdi.crawl.dto.ProductType;
import kr.co.hdi.crawl.dto.SiteType;
import kr.co.hdi.crawl.factory.CrawlerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CrawlService {

    private final CrawlerFactory crawlerFactory;

    public void startCrawlingAndSave(SiteType siteType, ProductType productType, String url) {

        CrawlTarget target = new CrawlTarget(siteType, productType);
        Crawler crawler = crawlerFactory.getInstance(target);

        // TODO: 결과를 return + save 로직 책임 분리 (현재는 crawler 내부에서 처리중)
        crawler.start(url);
    }

}
