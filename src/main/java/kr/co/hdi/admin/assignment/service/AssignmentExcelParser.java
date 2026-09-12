package kr.co.hdi.admin.assignment.service;

import kr.co.hdi.admin.assignment.dto.query.TeamAssignmentBlock;
import kr.co.hdi.admin.assignment.exception.AssignmentErrorCode;
import kr.co.hdi.admin.assignment.exception.AssignmentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
"시디/산디 전문가-ID매칭목록" 양식 파서.

양식 구조 (0-indexed):
- row 0: 팀 헤더 행. 팀 하나당 5컬럼을 차지한다.
    [팀명, "connect_id"(라벨), connect_id 값, "pw"(라벨), pw 값]
- row 1~2: 빈 행
- row 3: 데이터 목록 컬럼 헤더 ("아이디", "부문", "주체", "분류", 빈칸) - 팀 블록마다 반복
- row 4~ : 실제 매칭 데이터. "아이디" 칸이 비면 그 팀 블록의 데이터는 끝난 것으로 본다.

팀 블록은 최대 10개(팀A~팀J)까지 지원하며, connect_id가 비어있는 블록은 건너뛴다.
 */
@Component
public class AssignmentExcelParser {

    private static final int TEAM_COUNT = 10;
    private static final int BLOCK_WIDTH = 5;
    private static final int TEAM_HEADER_ROW = 0;
    private static final int COLUMN_HEADER_ROW = 3;
    private static final int DATA_START_ROW = 4;

    public List<TeamAssignmentBlock> parse(MultipartFile file) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            List<TeamAssignmentBlock> blocks = new ArrayList<>();

            for (int teamIndex = 0; teamIndex < TEAM_COUNT; teamIndex++) {
                int blockStartCol = teamIndex * BLOCK_WIDTH;
                TeamAssignmentBlock block = parseBlock(sheet, blockStartCol);
                if (!block.isEmpty()) {
                    blocks.add(block);
                }
            }
            return blocks;
        } catch (IOException e) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_EXCEL_FORMAT, "엑셀 파일을 읽을 수 없습니다.");
        }
    }

    private TeamAssignmentBlock parseBlock(Sheet sheet, int blockStartCol) {
        String team = cellToString(getCell(sheet, TEAM_HEADER_ROW, blockStartCol));
        String connectId = cellToString(getCell(sheet, TEAM_HEADER_ROW, blockStartCol + 2));
        String password = cellToString(getCell(sheet, TEAM_HEADER_ROW, blockStartCol + 4));

        List<String> dataCodes = new ArrayList<>();
        int row = DATA_START_ROW;
        while (true) {
            String code = cellToString(getCell(sheet, row, blockStartCol));
            if (code == null || code.isBlank()) {
                break;
            }
            dataCodes.add(code.trim());
            row++;
        }

        return new TeamAssignmentBlock(
                blank(team) ? null : team.trim(),
                blank(connectId) ? null : connectId.trim(),
                blank(password) ? null : password.trim(),
                dataCodes
        );
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private Cell getCell(Sheet sheet, int rowIdx, int colIdx) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) return null;
        return row.getCell(colIdx);
    }

    /*
    "아이디" 같은 코드 값은 숫자로 입력돼도(예: 3) 원래 형태(예: "0003")를 잃지 않도록
    서식 그대로 문자열로 뽑아내려 시도한다. 숫자 셀은 소수점/지수 표기 없이 정수 문자열로 변환한다.
     */
    private String cellToString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double value = cell.getNumericCellValue();
                if (value == Math.floor(value) && !Double.isInfinite(value)) {
                    yield String.valueOf((long) value);
                }
                yield String.valueOf(value);
            }
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }
}
