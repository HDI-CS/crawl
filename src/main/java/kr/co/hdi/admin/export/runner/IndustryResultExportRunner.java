package kr.co.hdi.admin.export.runner;

import kr.co.hdi.admin.export.service.IndustryResultExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("export-industry-result")
@RequiredArgsConstructor
public class IndustryResultExportRunner implements CommandLineRunner {

    private final IndustryResultExportService service;

    @Override
    public void run(String... args) {
        service.export(
                13L,  // assessment_round_id
                13L,  // year_id
                "/Users/choijeong-in/Downloads/industry_result_package_0810.xlsx"
        );
    }
}
