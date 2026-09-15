package com.webjob.application.service.JobRecommendation;

import com.webjob.application.exception.Customs.CvProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.hibernate.annotations.Comment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class CvExtractionService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final Tika tika = new Tika();

    private static final int MAX_CV_TEXT_LENGTH = 12000;


    //     Tải file CV về (Cloudinary URL) và trích xuất raw text.
    public String extractRawText(String cvUrl) {
        try {
            byte[] fileBytes = restTemplate.getForObject(cvUrl, byte[].class);

            if (fileBytes == null || fileBytes.length == 0) {
                throw new CvProcessingException("CV file rỗng hoặc không tải được: " + cvUrl);
            }

            try (InputStream is = new ByteArrayInputStream(fileBytes)) {
                String text = tika.parseToString(is);

                if (text == null || text.isBlank()) {
                    throw new CvProcessingException("Không trích xuất được nội dung từ CV");
                }

                return text.trim();
            }

        } catch (IOException | TikaException e) {
            log.error("Lỗi trích xuất CV: {}", cvUrl, e);
            throw new CvProcessingException("Không thể xử lý file CV", e);
        }
    }

    public String truncateForAi(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "";
        }

        text = text.trim();

        if (text.length() <= maxLength) {
            return text;
        }

        int truncateAt = text.lastIndexOf(' ', maxLength);

        if (truncateAt <= 0) {
            truncateAt = maxLength;
        }

        return text.substring(0, truncateAt)
                + "\n\n[CV content truncated for AI processing]";
    }


}
