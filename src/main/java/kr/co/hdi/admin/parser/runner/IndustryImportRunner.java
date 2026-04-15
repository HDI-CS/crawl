//package kr.co.hdi.admin.parser.runner;
//
//import kr.co.hdi.admin.parser.service.EarphoneImportService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Profile;
//import org.springframework.stereotype.Component;
//
//import java.nio.file.Path;
//
//
//@Component
//@Profile("import")
//@RequiredArgsConstructor
//public class IndustryImportRunner implements CommandLineRunner {
//
//    private final EarphoneImportService service;
//
//    @Override
//    public void run(String... args) {
//
//        service.importWaterproof(
//                Path.of("/Users/choijeong-in/Downloads/industry/data.xlsx"),
//                10L
//        );
//
//        System.out.println("✅ IMPORT 완료");
//    }
//}