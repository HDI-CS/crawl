package kr.co.hdi.survey.service;

import kr.co.hdi.survey.repository.ProductSurveyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SurveyService {

    private final ProductSurveyRepository productSurveyRepository;


}
