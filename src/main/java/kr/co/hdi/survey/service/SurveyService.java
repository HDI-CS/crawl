package kr.co.hdi.survey.service;

import kr.co.hdi.dataset.domain.BrandDatasetAssignment;
import kr.co.hdi.dataset.domain.ProductDatasetAssignment;
import kr.co.hdi.dataset.repository.BrandDatasetAssignmentRepository;
import kr.co.hdi.dataset.repository.ProductDatasetAssignmentRepository;
import kr.co.hdi.survey.domain.BrandResponse;
import kr.co.hdi.survey.domain.ProductResponse;
import kr.co.hdi.survey.dto.response.SurveyDataResponse;
import kr.co.hdi.survey.repository.BrandResponseRepository;
import kr.co.hdi.survey.repository.ProductResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SurveyService {

    private final BrandResponseRepository brandResponseRepository;
    private final ProductResponseRepository productResponseRepository;
    private final BrandDatasetAssignmentRepository brandDatasetAssignmentRepository;
    private final ProductDatasetAssignmentRepository productDatasetAssignmentRepository;

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
                .map(br -> new SurveyDataResponse(
                        br.getBrand().getBrandName(),
                        br.getBrand().getImage(),
                        br.getResponseStatus(),
                        br.getId()
                ))
                .toList();
    }

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
                .map(pr -> new SurveyDataResponse(
                        pr.getProduct().getProductName(),
                        "",   // TODO: 비측면 이미지
                        pr.getResponseStatus(),
                        pr.getId()
                ))
                .toList();
    }
}
