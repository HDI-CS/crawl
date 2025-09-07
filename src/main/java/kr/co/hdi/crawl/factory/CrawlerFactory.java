package kr.co.hdi.crawl.factory;


import kr.co.hdi.crawl.Crawler;
import kr.co.hdi.crawl.dto.CrawlTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CrawlerFactory {

    private final List<Crawler> crawlers;


    public Crawler getInstance(CrawlTarget target) {
        return crawlers.stream()
                .filter(crawler -> crawler.supports(target))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("")); // TODO: 커스텀 예외 추가
    }

}
