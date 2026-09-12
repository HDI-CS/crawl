package kr.co.hdi.admin.assignment.dto.response;

import java.util.List;

public record AssignmentImportResultResponse(
        int teamsProcessed,
        int assignmentsAdded,
        int assignmentsRemoved,
        List<String> warnings
) {
}
