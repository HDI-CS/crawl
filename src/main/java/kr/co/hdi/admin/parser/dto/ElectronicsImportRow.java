package kr.co.hdi.admin.parser.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ElectronicsImportRow {
    private final String code;
    private final String imageName;
    private final String companyName;
    private final String productName;      // model
    private final String productPath;      // category (breadcrumb)
    private final String productTypeName;  // type
    private final String usage;
    private final String weight;           // weight_g
    private final String price;            // price_krw
    private final String registeredAt;     // release_date
    private final String referenceUrl;     // store_url
    private final String industryDataCategory; // eesp/eebm/... 코드로부터 파생
}