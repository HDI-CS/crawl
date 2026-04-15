package kr.co.hdi.admin.parser.runner;//package kr.co.hdi.admin.parser.runner;
//
//import kr.co.hdi.admin.parser.service.SurveyImportService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//import java.nio.file.Path;
//
//@Component
//@RequiredArgsConstructor
//public class SurveyRunner implements CommandLineRunner {
//
//    private final SurveyImportService service;
//
//    @Override
//    public void run(String... args) throws Exception {
//
//        Path path = Path.of("/Users/choijeong-in/Downloads/설문.xlsx");
//
//        service.importSurvey(path, 11L); // yearId
//
//        System.out.println("🔥 실행 완료");
//    }
//}