package kr.co.hdi.survey.service;

import kr.co.hdi.dataset.domain.BrandDatasetAssignment;
import kr.co.hdi.dataset.domain.ProductDatasetAssignment;
import kr.co.hdi.dataset.repository.BrandDatasetAssignmentRepository;
import kr.co.hdi.dataset.repository.ProductDatasetAssignmentRepository;
import kr.co.hdi.survey.domain.*;
import kr.co.hdi.survey.dto.request.SurveyResponseRequest;
import kr.co.hdi.survey.dto.request.WeightedScoreRequest;
import kr.co.hdi.survey.dto.response.BrandDatasetResponse;
import kr.co.hdi.survey.dto.response.BrandSurveyDetailResponse;
import kr.co.hdi.survey.dto.response.BrandSurveyResponse;
import kr.co.hdi.survey.dto.response.SurveyDataResponse;
import kr.co.hdi.survey.exception.SurveyErrorCode;
import kr.co.hdi.survey.exception.SurveyException;
import kr.co.hdi.survey.repository.BrandResponseRepository;
import kr.co.hdi.survey.repository.BrandSurveyRepository;
import kr.co.hdi.survey.repository.ProductResponseRepository;
import kr.co.hdi.survey.repository.WeightedScoreRepository;
import kr.co.hdi.user.domain.UserEntity;
import kr.co.hdi.user.exception.AuthErrorCode;
import kr.co.hdi.user.exception.AuthException;
import kr.co.hdi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SurveyService {

    private final UserRepository userRepository;
    private final BrandSurveyRepository brandSurveyRepository;
    private final BrandResponseRepository brandResponseRepository;
    private final WeightedScoreRepository weightedScoreRepository;
    private final ProductResponseRepository productResponseRepository;
    private final BrandDatasetAssignmentRepository brandDatasetAssignmentRepository;
    private final ProductDatasetAssignmentRepository productDatasetAssignmentRepository;

    // 평가할 브랜드 리스트 조회
    @Transactional
    public List<SurveyDataResponse> getAllBrandSurveys(Long userId) {

        List<BrandResponse> brandResponses = brandResponseRepository.findAllByUserId(userId);
        if (brandResponses.isEmpty()) {
            List<BrandDatasetAssignment> assignments = brandDatasetAssignmentRepository.findAllByUserId(userId);
            List<BrandResponse> newResponses = assignments.stream()
                    .map(a -> BrandResponse.createBrandResponse(a.getUser(), a.getBrand()))
                    .toList();
            brandResponses = brandResponseRepository.saveAll(newResponses);
        }

        return brandResponses.stream()
                .sorted(Comparator.comparing(br -> br.getBrand().getId()))
                .map(br -> new SurveyDataResponse(
                        br.getBrand().getBrandName(),
                        br.getBrand().getImage(),
                        br.getResponseStatus(),
                        br.getId()
                ))
                .toList();
    }

    // 평가할 제품 리스트 조회
    @Transactional
    public List<SurveyDataResponse> getAllProductSurveys(Long userId) {

        List<ProductResponse> productResponses = productResponseRepository.findAllByUserId(userId);
        if (productResponses.isEmpty()) {
            List<ProductDatasetAssignment> assignments = productDatasetAssignmentRepository.findAllByUserId(userId);
            List<ProductResponse> newResponses = assignments.stream()
                    .map(a -> ProductResponse.createProductResponse(a.getUser(), a.getProduct()))
                    .toList();
            productResponses = productResponseRepository.saveAll(newResponses);
        }

        return productResponses.stream()
                .sorted(Comparator.comparing(pr -> pr.getProduct().getId()))
                .map(pr -> new SurveyDataResponse(
                        pr.getProduct().getProductName(),
                        "",   // TODO: 비측면 이미지
                        pr.getResponseStatus(),
                        pr.getId()
                ))
                .toList();
    }


    // 브랜드 평가 데이터셋 + 응답 조회
    public BrandSurveyDetailResponse getBrandSurveyDetail(Long brandResponseId) {

        BrandResponse brandResponse = brandResponseRepository.findById(brandResponseId)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.BRAND_RESPONSE_NOT_FOUND));

        BrandSurvey brandSurvey = brandSurveyRepository.findById(1L)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.SURVEY_NOT_FOUND));

        BrandDatasetResponse brandDatasetResponse = BrandDatasetResponse.fromEntity(brandResponse.getBrand());
        String dataId = brandResponse.getBrand().getBrandCode() + "_" + brandResponse.getBrand().getSectorCategory();
        BrandSurveyResponse brandSurveyResponse = BrandSurveyResponse.fromEntity(dataId, brandSurvey,brandResponse);
        return new BrandSurveyDetailResponse(brandDatasetResponse, brandSurveyResponse);
    }

    // 브랜드 응답 저장
    @Transactional
    public void saveBrandSurveyResponse(Long brandResponseId, SurveyResponseRequest request) {

        BrandResponse brandResponse = brandResponseRepository.findById(brandResponseId)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.BRAND_RESPONSE_NOT_FOUND));

        // 정량 평가
        if (request.index() != null) {
            brandResponse.updateResponse(request.index(), request.response());
        }

        // 정성 평가
        brandResponse.updateTextResponse(request.textResponse());

        brandResponse.updateResponseStatus();
        brandResponseRepository.save(brandResponse);
    }

    // 브랜드 응답 최종 제출
    @Transactional
    public void setBrandResponseStatusDone(Long brandResponseId, Long userId) {

        BrandResponse brandResponse = brandResponseRepository.findById(brandResponseId)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.BRAND_RESPONSE_NOT_FOUND));

        if (!brandResponse.checkAllResponsesFilled())
            throw new SurveyException(SurveyErrorCode.INCOMPLETE_RESPONSE);

        brandResponse.updateResponseStatusToDone();
        brandResponseRepository.save(brandResponse);

        // 모든 설문에 응답했는지
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        long datasetCount = brandDatasetAssignmentRepository.countByUser(user);
        long responsedDatasetCount = brandResponseRepository.countByUserAndResponseStatus(user, ResponseStatus.DONE);
        if (datasetCount == responsedDatasetCount)
            user.updateSurveyDoneStatus();
        userRepository.save(user);
    }

    // 가중치 평가
    @Transactional
    public void saveWeightedScores(Long userId, List<WeightedScoreRequest> requests) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        List<WeightedScore> scores = new ArrayList<>();
        for (WeightedScoreRequest request : requests) {
            scores.add(
                    WeightedScore.createWeightedScore(
                            user,
                            request.category(),
                            request.score1(),
                            request.score2(),
                            request.score3(),
                            request.score4(),
                            request.score5(),
                            request.score6(),
                            request.score7(),
                            request.score8())
            );
        }
        weightedScoreRepository.saveAll(scores);
    }
}
