package kr.co.hdi.admin.assignment.service;

import kr.co.hdi.admin.assignment.dto.request.AssignmentDataRequest;
import kr.co.hdi.admin.assignment.dto.response.AssessmentRoundResponse;
import kr.co.hdi.admin.assignment.dto.response.AssignmentImportResultResponse;
import kr.co.hdi.admin.assignment.dto.response.AssignmentResponse;
import kr.co.hdi.admin.data.dto.request.DataIdsRequest;
import kr.co.hdi.admin.data.dto.response.YearResponse;
import kr.co.hdi.admin.survey.dto.response.SurveyResponse;
import kr.co.hdi.admin.user.dto.response.ExpertNameResponse;
import kr.co.hdi.domain.user.entity.UserType;
import kr.co.hdi.domain.year.enums.DomainType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AssignmentService {

    DomainType getDomainType();
    List<ExpertNameResponse> searchExpertByName(UserType type, String q);
    List<SurveyResponse> getAssignmentYearRoundList(DomainType type);
    List<AssignmentResponse> getDatasetAssignment(Long assessmentRoundId, String q);
    AssignmentResponse getDatasetAssignmentByUser(Long assessmentRoundId, Long userId);
    void updateDatasetAssignment(Long assessmentRoundId, Long memberId, DataIdsRequest request);
    void createDatasetAssignment(Long assessmentRoundId, AssignmentDataRequest request);

    AssignmentImportResultResponse importDatasetAssignmentExcel(Long assessmentRoundId, MultipartFile file);

    byte[] exportDataAssignments(Long assessmentRoundId);
}
