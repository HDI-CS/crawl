package kr.co.hdi.crawl;

import kr.co.hdi.crawl.dto.CrawlTarget;

public interface Crawler {
    void start(String url);
    boolean supports(CrawlTarget target);
}
