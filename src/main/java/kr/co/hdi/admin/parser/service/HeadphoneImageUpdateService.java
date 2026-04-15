package kr.co.hdi.admin.parser.service;


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
import java.util.Set;
@Service
@RequiredArgsConstructor
@Transactional
public class HeadphoneImageUpdateService {

    private final IndustryDataRepository repository;
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
                String rawCode = folderName.split("_")[0];
                String code = String.format("%04d", Integer.parseInt(rawCode));

                try {
                    List<IndustryData> list = repository
                            .findAllByOriginalIdAndIndustryDataCategory(
                                    code,
                                    IndustryDataCategory.EARPHONE
                            );

                    if (list.isEmpty()) {
                        System.out.println("❌ DB 없음: " + code);
                        continue;
                    }

                    UploadResult detail = upload(folder, "dt");
                    UploadResult front  = upload(folder, "main");
                    UploadResult side   = upload(folder, "sub_01");
                    UploadResult side2  = upload(folder, "sub_02");
                    UploadResult side3  = upload(folder, "sub_03");

                    for (IndustryData data : list) {

                        data.updateImages(
                                getKey(detail),
                                getKey(front),
                                getKey(side),
                                getKey(side2),
                                getKey(side3),

                                getName(detail),
                                getName(front),
                                getName(side),
                                getName(side2),
                                getName(side3)
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

    private UploadResult upload(Path folder, String type) {

        try {
            Path file = Files.list(folder)
                    .filter(p -> p.getFileName().toString().toLowerCase()
                            .contains("_" + type))
                    .findFirst()
                    .orElse(null);

            if (file == null) {
                System.out.println("❌ 파일 없음: " + type);
                return null;
            }

            String fileName = file.getFileName().toString();
            String key = "2026/VI/" + fileName;

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

    private String getKey(UploadResult r) {
        return r == null ? null : r.key();
    }

    private String getName(UploadResult r) {
        return r == null ? null : r.fileName();
    }

    private record UploadResult(String key, String fileName) {}
}