package kr.co.hdi.admin.parser.service;

import kr.co.hdi.admin.parser.dto.WaterproofExcelParser;
import kr.co.hdi.admin.parser.dto.WaterproofRow;
import kr.co.hdi.domain.data.entity.IndustryData;
import kr.co.hdi.domain.data.enums.IndustryDataCategory;
import kr.co.hdi.domain.data.repository.IndustryDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;
@Service
@RequiredArgsConstructor
@Transactional
public class EarphoneImportService {

    private final WaterproofExcelParser parser;
    private final IndustryDataRepository repository;

    public void importWaterproof(Path excelPath, Long yearId) {
        System.out.println("🔥 importWaterproof 실행됨");
        List<WaterproofRow> rows = parser.parse(excelPath);

        for (WaterproofRow row : rows) {
            String code = row.code();
            if (code == null) continue;

            List<IndustryData> list =
                    repository.findAllByOriginalIdAndIndustryDataCategory(
                            code,
                            IndustryDataCategory.EARPHONE
                    );
            String chargeTime = clean(row.chargeTime());
            for (IndustryData data : list) {
                data.updateChargeTime(chargeTime);
            }

        }
    }

//    private String normalizeCode(String code) {
//        if (code == null) return null;
//        code = code.trim();
//        if (code.isBlank()) return null;
//        return code.replaceFirst("^0+", "");
//    }

    private String clean(String value) {
        if (value == null) return null;
        return value.trim();
    }
}