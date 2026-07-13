package kr.co.hdi.admin.parser;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import kr.co.hdi.domain.data.entity.IndustryData;
import kr.co.hdi.domain.data.enums.IndustryDataCategory;
import kr.co.hdi.domain.data.repository.IndustryDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class ElectronicsImageUpdateService {

    private final IndustryDataRepository repository;
    private final AmazonS3 amazonS3;

    @Value("${aws.s3.bucket}")
    private String bucket;

    private static final Pattern IMAGE_CODE_PATTERN = Pattern.compile("_([a-z]{4})_");

    private static final Map<String, IndustryDataCategory> CODE_TO_CATEGORY = Map.of(
            "eesp", IndustryDataCategory.BLUETOOTH_SPEAKER,
            "eebm", IndustryDataCategory.WIRELESS_MOUSE,
            "eeup", IndustryDataCategory.UMPC,
            "eeca", IndustryDataCategory.CAMERA,
            "eecm", IndustryDataCategory.WEBCAM,
            "eevc", IndustryDataCategory.PROJECTOR
    );

    /**
     * targetCodes가 null이거나 비어있으면 전체 폴더 처리 (기존 동작 그대로).
     * targetCodes에 값이 있으면, 그 코드(4자리, 예: "0459")에 해당하는 폴더만 처리.
     */
    public void updateImages(String imageRootStr, Long yearId, Set<String> targetCodes) {
        Path root = Paths.get(imageRootStr);

        try {
            List<Path> categoryFolders = Files.list(root)
                    .filter(Files::isDirectory)
                    .toList();

            for (Path categoryFolder : categoryFolders) {
                List<Path> productFolders = Files.list(categoryFolder)
                        .filter(Files::isDirectory)
                        .toList();

                for (Path folder : productFolders) {
                    String folderName = folder.getFileName().toString();
                    String rawCode = folderName.split("_")[0];
                    String code = String.format("%04d", Integer.parseInt(rawCode));

                    // 대상 코드 필터링 - 지정된 목록이 있으면 그것만 처리
                    if (targetCodes != null && !targetCodes.isEmpty() && !targetCodes.contains(code)) {
                        continue;
                    }

                    try {
                        IndustryDataCategory category = resolveCategoryFromFolder(folder);
                        System.out.println("🔍 " + folderName + " -> category=" + category);  // 추가

                        if (category == null) {
                            System.out.println("❌ 카테고리 파악 불가: " + categoryFolder.getFileName() + "/" + folderName);
                            continue;
                        }

                        List<IndustryData> list = repository
                                .findAllByYearIdAndCategoryAndOriginalId(yearId, category, code);

                        System.out.println("🔍 " + code + " -> DB 매칭 건수=" + list.size());  // 추가

                        if (list.isEmpty()) {
                            System.out.println("❌ DB 없음: " + code + " (" + category + ")");
                            continue;
                        }

                        UploadResult detail = upload(folder, "dt");
                        UploadResult front  = upload(folder, "main");
                        UploadResult side   = upload(folder, "sub_01");
                        UploadResult side2  = upload(folder, "sub_02");
                        UploadResult side3  = upload(folder, "sub_03");

                        for (IndustryData data : list) {
                            data.updateImageKeys(
                                    getKey(detail), getKey(front), getKey(side), getKey(side2), getKey(side3),
                                    getName(detail), getName(front), getName(side), getName(side2), getName(side3)
                            );
                        }

                        System.out.println("✅ 업데이트 완료: " + code);

                    } catch (Exception e) {
                        System.out.println("❌ 실패: " + code);
                        e.printStackTrace();
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("이미지 업데이트 실패", e);
        }
    }

    // 하위호환용 - 전체 처리
    public void updateImages(String imageRootStr, Long yearId) {
        updateImages(imageRootStr, yearId, null);
    }

    private IndustryDataCategory resolveCategoryFromFolder(Path folder) throws Exception {
        return Files.list(folder)
                .map(p -> {
                    Matcher m = IMAGE_CODE_PATTERN.matcher(p.getFileName().toString());
                    return m.find() ? CODE_TO_CATEGORY.get(m.group(1)) : null;
                })
                .filter(c -> c != null)
                .findFirst()
                .orElse(null);
    }

    private UploadResult upload(Path folder, String type) {
        try {
            Path file = Files.list(folder)
                    .filter(p -> p.getFileName().toString().toLowerCase().contains("_" + type))
                    .findFirst()
                    .orElse(null);

            if (file == null) {
                System.out.println("❌ 파일 없음: " + type);
                return null;
            }

            String fileName = file.getFileName().toString();
            String key = "2026/ID/" + fileName;

            try (InputStream in = Files.newInputStream(file)) {
                ObjectMetadata meta = new ObjectMetadata();
                meta.setContentLength(Files.size(file));
                amazonS3.putObject(bucket, key, in, meta);
                System.out.println("✅ 업로드: " + key);
                return new UploadResult(key, fileName);
            }
        } catch (Exception e) {
            throw new RuntimeException("S3 업로드 실패", e);
        }
    }

    private String getKey(UploadResult r) { return r == null ? null : r.key(); }
    private String getName(UploadResult r) { return r == null ? null : r.fileName(); }

    private record UploadResult(String key, String fileName) {}
}