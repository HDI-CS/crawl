package kr.co.hdi.admin.parser.runner;

import kr.co.hdi.admin.parser.service.EarphoneImportService;
import kr.co.hdi.admin.parser.service.VisualImageUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
@Component
@Profile("import")
@RequiredArgsConstructor
public class VisualImportRunner implements CommandLineRunner {

    private final VisualImageUpdateService service;

    @Override
    public void run(String... args) {

        service.updateImages(
                "/Users/choijeong-in/Downloads/visual/images"
        );

        System.out.println("✅ VISUAL 이미지 업데이트 완료");
    }
}