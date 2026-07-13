package kr.co.hdi.admin.parser;

import kr.co.hdi.admin.parser.ElectronicsImageUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Profile("import-image")
@RequiredArgsConstructor
public class ElectronicsImageUpdateRunner implements CommandLineRunner {

    private final ElectronicsImageUpdateService service;

    @Override
    public void run(String... args) {
        service.updateImages(
                "/Users/choijeong-in/Downloads/industry/images",
                13L,
                Set.of(
                        "0536", "0540", "0555", "0603", "0604", "0605", "0701",
                        "0706", "0721", "0737", "0745", "0765", "0800", "0807",
                        "0829", "0850", "0874", "0876", "0879", "0900", "0908",
                        "0911", "0928", "0941", "0991", "0998", "1002"
                )
        );
        System.out.println("✅ 선택 이미지 업데이트 완료 (27건)");
    }
}