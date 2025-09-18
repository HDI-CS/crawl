package kr.co.hdi.crawl.kakao;

import com.opencsv.CSVReader;
import kr.co.hdi.crawl.dto.BrandNameAndImageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoLogoService {

    @Value("${storage.kakao.logo.input-csv}")
    private String inputCsvPath;

    @Value("${storage.kakao.logo.output-image}")
    private String outputImagePath;

    public void getLogoImage(String type) {

        List<BrandNameAndImageDto> dtos = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(inputCsvPath))) {
            List<String[]> rows = reader.readAll();

            // 첫 줄(헤더) 건너뛰기
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                String brand = row[0];
                String image = row[2];

                dtos.add(new BrandNameAndImageDto(brand, getOriginalImageUrl(image)));
            }
        } catch (Exception e) {
            log.error("csv 읽는 중 에러 발생", e);
        }
        saveImages(dtos, outputImagePath, type);
    }

     private String getOriginalImageUrl(String url) {

        try {
            // fname= 이 있으면 그 부분 추출
            int idx = url.indexOf("fname=");
            if (idx != -1) {
                String encoded = url.substring(idx + 6); // fname= 이후 문자열
                return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
            } else {
                return url;
            }
        } catch (Exception e) {
            log.error("fname 원본 url 추출 중 에러 발생", e);
            return url;
        }
    }

    public void saveImages(List<BrandNameAndImageDto> dtos, String fileStoragePath, String type) {
        int successCnt = 1;
        for(int i = 0; i < dtos.size(); i++) {

            // 이미지 다운로드
            try {
                String indexLabel = String.format("%03d", successCnt);

                String imagePath = downloadImage(
                        dtos.get(i).imageUrl(), // DTO에서 이미지 URL 가져오기
                        fileStoragePath,
                        indexLabel + "_" + dtos.get(i).name(),      // 파일 이름 접두사
                        type
                );
                successCnt++;
            } catch (IOException e) {
                log.warn("{} 이미지 다운로드 실패 ", dtos.get(i).imageUrl());
            }
        }
    }

    private String downloadImage(String imageUrl, String folderPath, String prefix, String type) throws IOException {

        // URL에서 파일 확장자 추출
        String fileExtension = getFileExtension(imageUrl);
        String fileName = type + prefix + fileExtension;  // type : BE(cosmetic) 또는 FB(food)
        String filePath = Paths.get(folderPath, fileName).toString();

        HttpURLConnection connection = null;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            // User-Agent 설정 (일부 사이트에서 필요)
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                outputStream = new FileOutputStream(filePath);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                return filePath;
            } else {
                log.warn("이미지 다운로드 실패 - HTTP 상태코드: {} URL: {}", responseCode, imageUrl);
                return null;
            }

        } finally {
            if (outputStream != null) {
                try { outputStream.close(); } catch (IOException e) { /* ignore */ }
            }
            if (inputStream != null) {
                try { inputStream.close(); } catch (IOException e) { /* ignore */ }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String getFileExtension(String imageUrl) {
        try {
            String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);

            if (fileName.contains("?")) {
                fileName = fileName.substring(0, fileName.indexOf("?"));
            }

            if (fileName.contains(".")) {
                String extension = fileName.substring(fileName.lastIndexOf("."));
                if (extension.matches("\\.(jpg|jpeg|png|gif|webp|bmp)$")) {
                    return extension;
                }
            }

            return ".jpg";
        } catch (Exception e) {
            return ".jpg";
        }
    }
}
