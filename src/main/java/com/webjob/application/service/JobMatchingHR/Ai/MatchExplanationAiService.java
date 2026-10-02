package com.webjob.application.service.JobMatchingHR.Ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.webjob.application.component.LLM.LLMGeminiClient;
import com.webjob.application.dto.Request.ScoringResult;
import com.webjob.application.dto.Request.SkillMatchOutcome;
import com.webjob.application.dto.record.DecisionSupport;
import com.webjob.application.dto.record.ExplanationResult;
import com.webjob.application.models.Entity.CandidateProfile;
import com.webjob.application.models.Entity.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchExplanationAiService {
    private static final String PROMPT_VERSION = "match-explain-v1";
    private final LLMGeminiClient llmGeminiClient;

    private static final String SYSTEM_INSTRUCTION = """
            Bạn là AI hỗ trợ HR phân tích mức độ phù hợp giữa ứng viên và Job.
            Vai trò của bạn chỉ là phân tích và giải thích dữ liệu đầu vào.
            Bạn KHÔNG được tự quyết định tuyển dụng, loại ứng viên hoặc đề xuất quyết định tuyển dụng.

            INPUT:
            - Job description và thông tin ứng viên đã được hệ thống chuẩn hóa.
            - Danh sách skills được phân loại thành:
              + matched: ứng viên đáp ứng skill.
              + partial: ứng viên chỉ đáp ứng một phần skill hoặc chưa đáp ứng đầy đủ yêu cầu.
              + missing: ứng viên chưa có hoặc không có bằng chứng đáp ứng skill.
            - score: điểm phù hợp đã được hệ thống tính trước theo một bộ quy tắc cố định.

            NGUYÊN TẮC BẮT BUỘC:

            1. SCORE
            - Không được tự tính lại score.
            - Không được thay đổi, điều chỉnh, làm tròn hoặc đề xuất score mới.
            - Không được suy luận score từ skills, experience, project hoặc bất kỳ dữ liệu nào khác.
            - Giá trị score trong INPUT là giá trị duy nhất và phải được giữ nguyên.
            - Khi giải thích score, chỉ được sử dụng các dữ liệu có trong INPUT.

            2. EVIDENCE / SOURCE OF TRUTH
            - Chỉ sử dụng thông tin xuất hiện trong INPUT.
            - Không được bổ sung, suy diễn hoặc bịa ra skill, kinh nghiệm, project, chứng chỉ,
              seniority, thời gian làm việc, trách nhiệm hoặc thành tích mà INPUT không cung cấp.
            - Không được coi một skill là matched nếu INPUT không đánh dấu skill đó là matched.
            - Không được suy ra rằng ứng viên có một skill chỉ vì skill đó liên quan hoặc thường đi
              cùng một skill khác.
            - Nếu INPUT không đủ thông tin để kết luận một điểm nào đó, không được suy đoán.

            3. STRENGTHS
            - Trả về 2-5 điểm mạnh nếu INPUT có đủ dữ liệu.
            - Chỉ dựa trên:
              + matched skills;
              + kinh nghiệm được cung cấp;
              + projects được cung cấp;
              + các thông tin tích cực khác có trực tiếp trong INPUT.
            - Ưu tiên những điểm mạnh có liên quan trực tiếp đến yêu cầu của Job.
            - Không biến partial hoặc missing skill thành strength.
            - Mỗi strength phải có căn cứ rõ ràng từ INPUT.

            4. CONCERNS
            - Trả về 2-5 điểm HR cần xem xét nếu INPUT có đủ dữ liệu.
            - Ưu tiên các required skills đang ở trạng thái missing hoặc partial.
            - Có thể đề cập khoảng cách giữa yêu cầu của Job và thông tin ứng viên nếu khoảng cách đó
              được thể hiện trực tiếp trong INPUT.
            - Không được tạo concern dựa trên giả định, định kiến hoặc suy đoán cá nhân.
            - Không được coi missing skill là bằng chứng ứng viên chắc chắn không có skill đó nếu INPUT
              chỉ cho biết không tìm thấy hoặc không cung cấp thông tin về skill đó.
            - Không sử dụng ngôn ngữ mang tính kết luận tuyển dụng như "không nên tuyển", "nên loại",
              "không phù hợp để tuyển" hoặc các kết luận tương tự.

            5. EXPLANATION
            - Phần giải thích phải nhất quán với score và dữ liệu INPUT.
            - Không được đưa ra nhận định trái ngược với trạng thái matched/partial/missing trong INPUT.
            - Không được tạo ra tiêu chí đánh giá mới ngoài các tiêu chí đã có trong INPUT.
            - Phân biệt rõ giữa:
              + ứng viên thực sự thiếu skill;
              + INPUT không cung cấp bằng chứng về skill đó.
            - Nếu INPUT không có đủ bằng chứng, sử dụng cách diễn đạt thận trọng như
              "chưa có thông tin/bằng chứng trong INPUT" thay vì khẳng định ứng viên không có.

            6. STYLE
            - Viết ngắn gọn, khách quan, rõ ràng và dễ đọc đối với HR.
            - Không dùng ngôn ngữ cảm tính hoặc phóng đại.
            - Không đưa ra dự đoán về khả năng làm việc, hiệu suất hoặc kết quả tuyển dụng nếu INPUT
              không cung cấp căn cứ trực tiếp.
            - Không lặp lại thông tin không cần thiết.

            7. OUTPUT
            - Chỉ trả về đúng response schema được yêu cầu.
            - Không thêm markdown, giải thích, nhận xét hoặc text bên ngoài response schema.
            - Tất cả các field trong response phải tuân thủ đúng kiểu dữ liệu và cấu trúc của schema.
            - Nếu schema yêu cầu field nhưng INPUT không đủ dữ liệu, sử dụng giá trị phù hợp theo schema
              thay vì tự suy diễn dữ liệu.
              
            8. DECISION SUPPORT
                  
          - Bạn cung cấp thông tin hỗ trợ HR trong quá trình xem xét ứng viên.
          - Bạn KHÔNG được đưa ra quyết định tuyển dụng.
          - Bạn KHÔNG được trả lời hoặc tạo ra kết luận như:
            + "nên tuyển"
            + "không nên tuyển"
            + "nên loại"
            + "nên mời phỏng vấn"
            + "không nên mời phỏng vấn"
            + "hire"
            + "reject"
            + "pass"
            + "fail"
            hoặc bất kỳ kết luận tương đương nào.
          
          - Decision support chỉ được mô tả:
            + những nội dung HR nên xác minh;
            + những skill/requirement nên tập trung trao đổi khi phỏng vấn;
            + những khoảng trống thông tin cần làm rõ;
            + những điểm trong INPUT cần HR xem xét thêm.
          
          - Mọi decision support phải dựa trực tiếp trên INPUT.
          - Không được tạo thêm tiêu chí đánh giá ngoài INPUT.
          - Không được suy luận rằng một ứng viên sẽ làm việc tốt hoặc không tốt.
          - Không được dự đoán kết quả tuyển dụng.
          - Không được biến score thành một quyết định tuyển dụng.
          
          - Nếu một required skill có trạng thái partial hoặc missing:
            + có thể đưa skill đó vào interviewFocus hoặc verificationItems;
            + phải phân biệt "chưa có bằng chứng trong INPUT" với "ứng viên thực sự không có skill".
          
          - Decision support phải giúp HR biết:
            1. Cần xác minh điều gì?
            2. Nên hỏi sâu về yêu cầu nào?
            3. Có khoảng trống thông tin nào trong INPUT?
                                

            MỤC TIÊU:
            Tạo một bản phân tích ngắn gọn, có căn cứ và có thể kiểm chứng từ INPUT,
            giúp HR hiểu score và những điểm cần xem xét, nhưng không thay HR đưa ra quyết định tuyển dụng.
            """;


    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "strengths", Map.of(
                            "type", "ARRAY",
                            "items", Map.of("type", "STRING"),
                            "minItems", 2,
                            "maxItems", 5
                    ),
                    "concerns", Map.of(
                            "type", "ARRAY",
                            "items", Map.of("type", "STRING"),
                            "minItems", 2,
                            "maxItems", 5
                    ),
                    "experienceAssessment", Map.of(
                            "type", "STRING"
                    ),
                    "educationAssessment", Map.of(
                            "type", "STRING"
                    ),
                    "overallReason", Map.of(
                            "type", "STRING"
                    ),
                    "decisionSupport", Map.of(
                            "type", "OBJECT",
                            "properties", Map.of(
                                    "interviewFocus", Map.of(
                                            "type", "ARRAY",
                                            "items", Map.of("type", "STRING"),
                                            "minItems", 0,
                                            "maxItems", 5
                                    ),
                                    "verificationItems", Map.of(
                                            "type", "ARRAY",
                                            "items", Map.of("type", "STRING"),
                                            "minItems", 0,
                                            "maxItems", 5
                                    ),
                                    "considerations", Map.of(
                                            "type", "ARRAY",
                                            "items", Map.of("type", "STRING"),
                                            "minItems", 0,
                                            "maxItems", 5
                                    )
                            ),
                            "required", List.of(
                                    "interviewFocus",
                                    "verificationItems",
                                    "considerations"
                            )
                    )

            ),
            "required", List.of(
                    "strengths",
                    "concerns",
                    "experienceAssessment",
                    "educationAssessment",
                    "overallReason",
                    "decisionSupport"
            )

    );

    public ExplanationResult explain(Job job, CandidateProfile profile, ScoringResult scoringResult) {
        String userContent = buildUserContent(job, profile, scoringResult);

        log.info("Starting AI explanation generation. jobId={}, profileId={}, userContentLength={}",
                job.getId(), profile.getId(), userContent != null ? userContent.length() : 0);

        LLMGeminiClient.LLMGeminiResult geminiResult = llmGeminiClient.callWithFallback(SYSTEM_INSTRUCTION, userContent, RESPONSE_SCHEMA);
        JsonNode json = geminiResult.json();

        JsonNode decisionSupportNode = json.path("decisionSupport");
        DecisionSupport decisionSupport = new DecisionSupport(
                toList(decisionSupportNode.path("interviewFocus")),
                toList(decisionSupportNode.path("verificationItems")),
                toList(decisionSupportNode.path("considerations"))
        );

        ExplanationResult result = new ExplanationResult(
                toList(json.path("strengths")),
                toList(json.path("concerns")),
                json.path("experienceAssessment").asText(""),
                json.path("educationAssessment").asText(""),
                json.path("overallReason").asText(""),
                decisionSupport,
                geminiResult.model(),
                PROMPT_VERSION
        );

        log.info("Successfully generated AI explanation. model={}, promptVersion={}, strengthsCount={}, concernsCount={}",
                geminiResult.model(), PROMPT_VERSION, result.strengths().size(), result.concerns().size());

        return result;
    }

    private String buildUserContent(Job job, CandidateProfile profile, ScoringResult scoringResult) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== THÔNG TIN CÔNG VIỆC ===\n")
                .append("Vị trí tuyển dụng: ").append(job.getName()).append("\n")
                .append("Số năm kinh nghiệm yêu cầu: ").append(job.getExperienceRequired()).append(" năm\n")
                .append("Địa điểm làm việc: ").append(job.getLocation()).append("\n")
                .append("Mô tả công việc: ").append(job.getDescription()).append("\n")
                .append("Trách nhiệm công việc: ").append(job.getResponsibility()).append("\n")
                .append("Yêu cầu công việc: ").append(job.getRequirement()).append("\n\n");

        sb.append("=== THÔNG TIN ỨNG VIÊN ===\n")
                .append("Tổng số năm kinh nghiệm: ")
                .append(profile.getTotalExperienceYears())
                .append(" năm\n")
                .append("Nội dung CV đã trích xuất:\n")
                .append(profile.getRawText())
                .append("\n\n");

        sb.append("=== KẾT QUẢ ĐỐI CHIẾU KỸ NĂNG - DỮ LIỆU BACKEND ===\n");
        sb.append("CHỈ phân tích và diễn giải dựa trên dữ liệu được cung cấp.\n");
        sb.append("KHÔNG được suy đoán, bổ sung hoặc bịa thêm kỹ năng, kinh nghiệm hay thông tin ứng viên.\n");
        sb.append("Nếu dữ liệu không đủ để kết luận, ghi rõ: KHÔNG ĐỦ DỮ LIỆU.\n\n");

        for (SkillMatchOutcome o : scoringResult.getSkillOutcomes()) {
            String jobSkillName = o.getJobSkill() != null
                    && o.getJobSkill().getSkill() != null
                    ? o.getJobSkill().getSkill().getName()
                    : "UNKNOWN";

            String requirementType = o.getJobSkill() != null
                    ? (o.getJobSkill().getRequired() ? "BẮT BUỘC" : "ƯU TIÊN")
                    : "UNKNOWN";

            String matchType = o.getMatchType() != null
                    ? o.getMatchType().toString()
                    : "UNKNOWN";

            String candidateSkillName = o.getCandidateSkill() != null
                    && o.getCandidateSkill().getSkill() != null
                    ? o.getCandidateSkill().getSkill().getName()
                    : "MISSING";

            sb.append("- Job Skill: ")
                    .append(jobSkillName)
                    .append("\n");

            sb.append("  Requirement: ")
                    .append(requirementType)
                    .append("\n");

            sb.append("  Match Type: ")
                    .append(matchType)
                    .append("\n");

            sb.append("  Candidate Skill: ")
                    .append(candidateSkillName)
                    .append("\n");

            if (o.getNote() != null && !o.getNote().isBlank()) {
                sb.append("  Backend Note: ")
                        .append(o.getNote())
                        .append("\n");
            } else {
                sb.append("  Backend Note: NONE\n");
            }

            sb.append("\n");
        }


        sb.append("\n=== ĐIỂM SỐ ĐÃ ĐƯỢC TÍNH TOÁN (KHÔNG ĐƯỢC THAY ĐỔI) ===\n")
                .append("Điểm phù hợp tổng thể: ").append(scoringResult.getMatchScore()).append("/100\n")
                .append("Điểm kỹ năng: ").append(scoringResult.getSkillScore()).append("/100\n")
                .append("Điểm kinh nghiệm: ").append(scoringResult.getExperienceScore()).append("/100\n")
                .append("Điểm học vấn: ").append(scoringResult.getEducationScore()).append("/100\n")
                .append("Điểm dự án: ").append(scoringResult.getProjectScore()).append("/100\n")
                .append("Điểm chứng chỉ: ").append(scoringResult.getCertificationScore()).append("/100\n");

        return sb.toString();
    }

    private List<String> toList(JsonNode arrayNode) {
        List<String> result = new ArrayList<>();
        arrayNode.forEach(n -> result.add(n.asText()));
        return result;
    }


}


