package kr.co.hdi.admin.parser.dto;

import org.springframework.stereotype.Component;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Component
public class WaterproofExcelParser {

    private static final Map<String, String> HEADER_MAP = Map.ofEntries(
            Map.entry("ID", "code"),
            Map.entry("충전시간(hr)", "chargeTime")
    );

    public List<WaterproofRow> parse(Path excelPath) {

        try (InputStream in = Files.newInputStream(excelPath);
             Workbook workbook = new XSSFWorkbook(in)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            Map<Integer, String> columnMap = new HashMap<>();

            // 👉 필요한 컬럼만 매핑
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                String header = get(headerRow, i);
                String field = HEADER_MAP.get(header);

                if (field != null) {
                    columnMap.put(i, field);
                }
            }

            List<WaterproofRow> result = new ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String code = "";
                String chargeTime = "";

                for (Map.Entry<Integer, String> e : columnMap.entrySet()) {
                    String value = get(row, e.getKey());

                    if (e.getValue().equals("code")) {
                        code = value;
                    }
                    if (e.getValue().equals("chargeTime")) {
                        chargeTime = value;
                    }
                }

                result.add(new WaterproofRow(code, chargeTime));
            }

            return result;

        } catch (Exception e) {
            throw new IllegalStateException("엑셀 파싱 실패", e);
        }
    }

    private String get(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}