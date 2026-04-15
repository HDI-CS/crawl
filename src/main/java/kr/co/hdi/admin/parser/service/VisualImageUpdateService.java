
package kr.co.hdi.admin.parser.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import kr.co.hdi.domain.data.entity.VisualData;
import kr.co.hdi.domain.data.enums.VisualDataCategory;
import kr.co.hdi.domain.data.repository.VisualDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VisualImageUpdateService {

    private final VisualDataRepository repository;
    private final AmazonS3 amazonS3;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public void updateImages(String imageRootStr) {

        Path root = Paths.get(imageRootStr);

        try {
            List<Path> folders = Files.list(root)
                    .filter(Files::isDirectory)
                    .toList();

            for (Path folder : folders) {

                String folderName = folder.getFileName().toString();

                // ex) 0188_name → 0188 → 0188
                String rawCode = folderName.split("_")[0];
                String code = String.format("%04d", Integer.parseInt(rawCode));

                try {
                    List<VisualData> list =
                            repository.findByBrandCodeAndVisualDataCategory(
                                    code,
                                    VisualDataCategory.POSTER
                            );

                    if (list.isEmpty()) {
                        System.out.println("❌ DB 없음: " + code);
                        continue;
                    }

                    // 🔥 파일 선택 (핵심)
                    Path file = findTargetImage(folder, code);

                    if (file == null) {
                        System.out.println("❌ 업로드할 파일 없음: " + code);
                        continue;
                    }

                    UploadResult logo = upload(file);

                    if (logo == null) {
                        System.out.println("❌ 업로드 실패: " + code);
                        continue;
                    }

                    for (VisualData data : list) {
                        data.updateImages(
                                logo.key(),
                                logo.fileName()
                        );
                    }

                    System.out.println("✅ 업데이트 완료: " + code);

                } catch (Exception e) {
                    System.out.println("❌ 실패: " + code);
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("이미지 업데이트 실패", e);
        }
    }

    /**
     * 🔥 핵심: 파일 선택 로직
     */
    private Path findTargetImage(Path folder, String code) throws Exception {

        List<Path> files = Files.list(folder)
                .filter(p -> !Files.isDirectory(p))
                .filter(this::isImageFile)
                .toList();

        System.out.println("📂 [" + code + "] 파일 목록:");
        files.forEach(p -> System.out.println("👉 " + p.getFileName()));

        // 1️⃣ code 포함 + VI_ 없는 파일 우선
        Path target = files.stream()
                .filter(p -> {
                    String name = p.getFileName().toString();
                    return name.contains(code) && !name.startsWith("VI_");
                })
                .findFirst()
                .orElse(null);

        if (target != null) {
            System.out.println("✅ 선택된 파일 (1순위): " + target.getFileName());
            return target;
        }

        // 2️⃣ code 포함 아무 파일
        target = files.stream()
                .filter(p -> p.getFileName().toString().contains(code))
                .findFirst()
                .orElse(null);

        if (target != null) {
            System.out.println("⚠️ 선택된 파일 (2순위): " + target.getFileName());
            return target;
        }

        // 3️⃣ fallback (파일명 정렬 후 첫번째)
        target = files.stream()
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .findFirst()
                .orElse(null);

        if (target != null) {
            System.out.println("⚠️ fallback 파일: " + target.getFileName());
        }

        return target;
    }

    /**
     * 이미지 파일 필터
     */
    private boolean isImageFile(Path p) {
        String name = p.getFileName().toString().toLowerCase();
        return name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png");
    }

    /**
     * S3 업로드
     */
    private UploadResult upload(Path file) {

        try {
            String fileName = file.getFileName().toString();

            // 👉 key 유지 (덮어쓰기)
            String key = "2026/VI/" + fileName;

            System.out.println("📤 업로드 파일: " + fileName);
            System.out.println("📤 S3 key: " + key);

            try (InputStream in = Files.newInputStream(file)) {

                ObjectMetadata meta = new ObjectMetadata();
                meta.setContentLength(Files.size(file));

                amazonS3.putObject(bucket, key, in, meta);

                System.out.println("✅ S3 업로드 성공");

                return new UploadResult(key, fileName);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private record UploadResult(String key, String fileName) {}
}