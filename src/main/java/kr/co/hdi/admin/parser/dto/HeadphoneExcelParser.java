//package kr.co.hdi.admin.parser.dto;
//
//
//
//import org.springframework.stereotype.Component;
//import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//import org.springframework.stereotype.Component;
//
//import java.io.InputStream;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.*;
//@Component
//public class HeadphoneExcelParser {
//
//    private static final Map<String, String> HEADER_MAP = Map.ofEntries(
//            Map.entry("번호", "code"),
//            Map.entry("회사명", "companyName"),
//            Map.entry("모델명", "productName"),
//            Map.entry("카테고리", "productPath"),
//            Map.entry("유형", "productTypeName"),
//            Map.entry("용도", "usage"),
//            Map.entry("노이즈캔슬링", "noiseCancelling"),
//            Map.entry("코덱", "codec"),
//            Map.entry("부가기능", "extraFeatures"),
//            Map.entry("컨트롤", "controlType"),
//            Map.entry("최대재생시간(hr)", "maxPlayTime"),
//            Map.entry("1회 충전시간(hr)", "chargeTime"),
//            Map.entry("무게(g)", "weight"),
//            Map.entry("출시 가격(원)", "price"),
//            Map.entry("출시 날짜", "registeredAt"),
//            Map.entry("판매 웹페이지", "referenceUrl")
//
//
//    );
//
//    public List<HeadphoneImportRow> parse(Path excelPath) {
//
//        try (InputStream in = Files.newInputStream(excelPath);
//             Workbook workbook = new XSSFWorkbook(in)) {
//
//            Sheet sheet = workbook.getSheetAt(0);
//            Row headerRow = sheet.getRow(0);
//
//            Map<Integer, String> columnMap = new HashMap<>();
//
//            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
//                String header = get(headerRow, i);
//                String field = HEADER_MAP.get(header);
//
//                if (field != null) {
//                    columnMap.put(i, field);
//                }
//            }
//
//            List<HeadphoneImportRow> result = new ArrayList<>();
//
//            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//
//                Map<String, String> map = new HashMap<>();
//
//                for (Map.Entry<Integer, String> e : columnMap.entrySet()) {
//                    map.put(e.getValue(), get(row, e.getKey()));
//                }
//
//                result.add(
//                        HeadphoneImportRow.builder()
//                                .code(map.get("code"))
//                                .companyName(map.get("companyName"))
//                                .productName(map.get("productName"))
//                                .productPath(map.get("productPath"))
//                                .productTypeName(map.get("productTypeName"))
//                                .usage(map.get("usage"))
//                                .noiseCancelling(map.get("noiseCancelling"))
//                                .codec(map.get("codec"))
//                                .extraFeatures(map.get("extraFeatures"))
//                                .controlType(map.get("controlType"))
//                                .maxPlayTime(map.get("maxPlayTime"))
//                                .chargeTime(map.get("chargeTime"))
//                                .weight(map.get("weight"))
//                                .price(map.get("price"))
//                                .registeredAt(map.get("registeredAt"))
//                                .referenceUrl(map.get("referenceUrl"))
//                                .build()
//                );
//            }
//
//            return result;
//
//        } catch (Exception e) {
//            throw new IllegalStateException("엑셀 파싱 실패", e);
//        }
//    }
//
//    private String get(Row row, int idx) {
//        Cell cell = row.getCell(idx);
//        if (cell == null) return "";
//
//        return switch (cell.getCellType()) {
//            case STRING -> cell.getStringCellValue().trim();
//            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
//            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
//            default -> "";
//        };
//    }
//}