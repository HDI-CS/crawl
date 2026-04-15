package kr.co.hdi.admin.parser.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
public record WaterproofRow(
        String code,
        String chargeTime
) {}
