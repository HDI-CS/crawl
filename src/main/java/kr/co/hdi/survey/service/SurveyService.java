package kr.co.hdi.survey.service;

import kr.co.hdi.crawl.repository.ProductImageRepository;
import kr.co.hdi.dataset.domain.BrandDatasetAssignment;
import kr.co.hdi.dataset.domain.ProductDatasetAssignment;
import kr.co.hdi.dataset.repository.BrandDatasetAssignmentRepository;
import kr.co.hdi.dataset.repository.ProductDatasetAssignmentRepository;
import kr.co.hdi.survey.domain.*;
import kr.co.hdi.survey.dto.response.*;
import kr.co.hdi.survey.dto.request.SurveyResponseRequest;
import kr.co.hdi.survey.dto.request.WeightedScoreRequest;
import kr.co.hdi.survey.exception.SurveyErrorCode;
import kr.co.hdi.survey.exception.SurveyException;
import kr.co.hdi.survey.repository.*;
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
    private final ProductImageRepository productImageRepository;
    private final BrandDatasetAssignmentRepository brandDatasetAssignmentRepository;
    private final ProductDatasetAssignmentRepository productDatasetAssignmentRepository;
    private final ProductSurveyRepository productSurveyRepository;

    // 평가할 브랜드 리스트 조회
    @Transactional
    public List<ProductSurveyDataResponse> getAllBrandSurveys(Long userId) {

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
                .map(br -> new ProductSurveyDataResponse(
                        br.getBrand().getBrandName(),
                        br.getBrand().getImage(),
                        br.getResponseStatus(),
                        br.getId()
                ))
                .toList();
    }

    // 평가할 제품 리스트 조회
    @Transactional
    public List<ProductSurveyDataResponse> getAllProductSurveys(Long userId) {

        List<ProductResponse> productResponses = productResponseRepository.findAllByUserId(userId);
        if (productResponses.isEmpty()) {
            List<ProductDatasetAssignment> assignments = productDatasetAssignmentRepository.findAllByUserId(userId);
            List<ProductResponse> newResponses = assignments.stream()
                    .map(a -> ProductResponse.createProductResponse(a.getUser(), a.getProduct()))
                    .toList();
            productResponses = productResponseRepository.saveAll(newResponses);
        }

        return productResponses.stream()
                .sorted(Comparator.comparing(productResponse -> productResponse.getProduct().getId()))
                .map(productResponse -> new ProductSurveyDataResponse(
                        productResponse.getProduct().getProductName(),
                        productImageRepository.findByProductId(productResponse.getProduct().getId()).getFrontPath(),
                        productResponse.getResponseStatus(),
                        productResponse.getId()
                ))
                .toList();
    }

    public ProductSurveyDetailResponse getProductSurveyDetail(Long productResponseId) {
        ProductResponse productResponse = productResponseRepository.findById(productResponseId)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.PRODUCT_RESPONSE_NOT_FOUND));

        ProductSurvey productSurvey = productSurveyRepository.findById(1L)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.SURVEY_NOT_FOUND));

        ProductDataSetResponse dataSetResponse = ProductDataSetResponse.from(productResponse.getProduct(),
                productImageRepository.findByProductId(productResponse.getProduct().getId()));

        ProductSurveyResponse productSurveyResponse = ProductSurveyResponse.from(productSurvey, productResponse);

        return new ProductSurveyDetailResponse(dataSetResponse, productSurveyResponse);
    }


    @Transactional
    public void saveProductSurveyResponse(Long productResponseId, SurveyResponseRequest request, Long userId) {

        ProductResponse productResponse = productResponseRepository.findById(productResponseId)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.PRODUCT_RESPONSE_NOT_FOUND));

        checkUserAuthorization(productResponse, userId);

        // 정량 평가
        if (request.index() != null) {
            productResponse.updateResponse(request.index(), request.response());
        } else {  // 정성 평가
            productResponse.updateTextResponse(request.textResponse());
        }

        productResponse.updateResponseStatus();
        productResponseRepository.save(productResponse);
    }

    private void checkUserAuthorization(ProductResponse productResponse, Long userId) {
        if (!productResponse.getUser().getId().equals(userId)) {
            throw new SurveyException(SurveyErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    // 제품 응답 최종 제출
    @Transactional
    public void setProductResponseStatusDone(Long productResponseId, Long userId) {

        ProductResponse productResponse = productResponseRepository.findById(productResponseId)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.PRODUCT_RESPONSE_NOT_FOUND));

        if (!productResponse.checkAllResponsesFilled())
            throw new SurveyException(SurveyErrorCode.INCOMPLETE_RESPONSE);

        productResponse.updateResponseStatusToDone();
        productResponseRepository.save(productResponse);

        // 모든 설문에 응답했는지
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        long datasetCount = productDatasetAssignmentRepository.countByUser(user);
        long responsedDatasetCount = productResponseRepository.countByUserAndResponseStatus(user, ResponseStatus.DONE);
        if (datasetCount == responsedDatasetCount)
            user.updateSurveyDoneStatus();
        userRepository.save(user);
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
        } else {   // 정성 평가
            brandResponse.updateTextResponse(request.textResponse());
        }

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

            WeightedScore score;

            if (request.id() != null) {
                // 기존 엔티티 조회
                score = weightedScoreRepository.findById(request.id())
                        .orElseThrow(() -> new IllegalArgumentException("WeightedScore not found with id: " + request.id()));

                // 값 갱신
                score.updateScores(
                        request.score1(),
                        request.score2(),
                        request.score3(),
                        request.score4(),
                        request.score5(),
                        request.score6(),
                        request.score7(),
                        request.score8()
                );

            } else {
                // 새 엔티티 생성
                score = WeightedScore.createWeightedScore(
                        user,
                        request.category(),
                        request.score1(),
                        request.score2(),
                        request.score3(),
                        request.score4(),
                        request.score5(),
                        request.score6(),
                        request.score7(),
                        request.score8()
                );
            }
            scores.add(score);
//            scores.add(
//                    WeightedScore.createWeightedScore(
//                            user,
//                            request.category(),
//                            request.score1(),
//                            request.score2(),
//                            request.score3(),
//                            request.score4(),
//                            request.score5(),
//                            request.score6(),
//                            request.score7(),
//                            request.score8())
//            );
        }
        weightedScoreRepository.saveAll(scores);
    }

    public List<WeightedScoreResponse> getWeightedResponse(Long userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        List<WeightedScore> scores = weightedScoreRepository.findByUser(user);
        return scores.stream()
                .map(score -> new WeightedScoreResponse(
                        score.getId(),
                        score.getCategory(),
                        score.getScore1(),
                        score.getScore2(),
                        score.getScore3(),
                        score.getScore4(),
                        score.getScore5(),
                        score.getScore6(),
                        score.getScore7(),
                        score.getScore8()
                ))
                .toList();
    }
}
