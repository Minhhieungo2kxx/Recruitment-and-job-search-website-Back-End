package com.webjob.application.service.JobMatchingHR.Ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.webjob.application.component.LLM.LLMGeminiClient;
import com.webjob.application.dto.Request.CandidateProfileDetails;
import com.webjob.application.dto.Request.ParsedSkill;
import com.webjob.application.dto.record.CandidateProfileAiResult;
import com.webjob.application.enums.SkillLevel;
import com.webjob.application.utils.common.UtilFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateProfileAiService {
    private final LLMGeminiClient llmGeminiClient;
    private static final String PROMPT_VERSION = "cv-parse-v1";
    private static final String SYSTEM_INSTRUCTION = """
            Bạn là AI CV Parser cho hệ thống tuyển dụng ĐA NGÀNH.

            NHIỆM VỤ:
            Đọc CV dạng text thô (có thể lỗi format do PDF/Word/OCR) và trả về JSON đúng response schema.

            NGUYÊN TẮC:
            - Chỉ trích xuất thông tin có trong CV. KHÔNG suy diễn, phỏng đoán hoặc bịa.
            - Không dùng kiến thức bên ngoài để bổ sung dữ liệu cho ứng viên.
            - Nếu không có thông tin: dùng [] hoặc null theo schema.
            - Không tạo dữ liệu chỉ để làm đầy response.
            - Có thể bỏ qua lỗi OCR/format rõ ràng nếu chắc chắn không làm thay đổi ý nghĩa.
            - Không tự chuẩn hóa nội dung ứng viên viết.

            Đây là bản đã giữ **đúng format/căn lề**, đồng thời ngắn gọn và xử lý vấn đề `level = null`:
           
            + SKILLS:
            - Chỉ trích xuất skill được CV đề cập; giữ nguyên tên, không thêm/dịch/gộp/chuẩn hóa.
            - Mỗi skill gồm: name, yearsOfExperience, level.     
            - yearsOfExperience:
              - Là tổng thời gian có căn cứ cho thấy ứng viên sử dụng skill.
              - Được phép SUY RA từ thời gian của WORK EXPERIENCE / PROJECT nếu skill được
                gắn với công việc/dự án đó; không yêu cầu CV ghi trực tiếp số năm.
              - Ưu tiên bằng chứng trong WORK EXPERIENCE / PROJECT; không chỉ dựa vào
                TECHNICAL SKILLS hoặc ABOUT ME.
              - Chỉ tính khoảng thời gian skill thực sự được sử dụng.
              - Không double-count các khoảng thời gian bị overlap.
              - Làm tròn 1 chữ số.
              - Nếu chỉ biết skill nhưng không xác định được thời gian sử dụng từ
                EXPERIENCE/PROJECT → null.
              - Không suy ra thời gian sử dụng chỉ từ chức danh, bằng cấp hoặc việc skill
                xuất hiện trong danh sách TECHNICAL SKILLS.
                        
            - level CHỈ nhận: BASIC, INTERMEDIATE, ADVANCED, EXPERT.
            - Nếu skill đã được xác định là skill của ứng viên → level BẮT BUỘC phải có,
              KHÔNG được null.
            - BASIC = skill chỉ được đề cập/liệt kê hoặc có rất ít bằng chứng sử dụng.
            - INTERMEDIATE = có bằng chứng sử dụng thực tế trong công việc/dự án.
            - ADVANCED = có bằng chứng sử dụng chuyên sâu như design, optimization,
              troubleshooting hoặc scalability.
            - EXPERT = có bằng chứng rõ về architecture, technical leadership, mentoring
              hoặc technical ownership.
            - Nếu không đủ bằng chứng cho level cao hơn → chọn BASIC.
            - Không suy luận level chỉ từ chức danh hoặc số năm kinh nghiệm.
            - Không được trả level = null.

            TOTAL EXPERIENCE:
            - Tính tổng thời gian làm việc thực tế từ các khoảng kinh nghiệm trong CV.
            - Không tính thời gian học.
            - Không cộng trùng các khoảng thời gian chồng lấn.
            - Nếu chỉ biết tháng/năm, dùng ngày 01.
            - Làm tròn 1 chữ số thập phân.
            - Không suy đoán kinh nghiệm từ chức danh, tuổi hoặc thông tin khác.
            - Nếu không đủ dữ liệu, chỉ tính phần có căn cứ.

            DATES:
            - Output format yyyy-MM-dd khi CV cung cấp ngày/tháng cụ thể.
            - Nếu chỉ có tháng/năm → ngày 01.
            - Nếu chỉ có năm, giữ YEAR-ONLY trong quá trình tính toán; không giả định ngày/tháng cụ thể.
            - "2019 - 2020" được hiểu là khoảng kinh nghiệm kéo dài 1 năm.
            - Không được biến "2019 - 2020" thành "2019-01-01 - 2020-01-01" để tính toán nếu CV chỉ cung cấp năm.
                       

            EDUCATION / PROJECT RELEVANCE:
            - CV có thể thuộc BẤT KỲ ngành nào: IT, Finance, Marketing, HR, Legal,
              Healthcare, Engineering, Manufacturing, Education, Design, Logistics,
              Sales, Accounting, Architecture, Hospitality, Research, v.v.
            - KHÔNG mặc định relevance theo IT.
            - relevance CHỈ nhận: RELEVANT, PARTIAL, NOT_RELEVANT.
            - Có TARGET JOB / JOB DESCRIPTION / TARGET INDUSTRY → đánh giá theo target đó.
            - Không có target → đánh giá theo lĩnh vực nghề nghiệp rõ ràng nhất trong CV.
            - Không đủ căn cứ xác định lĩnh vực → PARTIAL.
            - RELEVANT = liên quan trực tiếp, rõ ràng.
            - PARTIAL = liên quan một phần/gián tiếp HOẶC chưa đủ căn cứ kết luận.
            - NOT_RELEVANT = không liên quan đáng kể.
            - Nếu có nhiều EDUCATION / PROJECT:
              + Có ít nhất 1 mục liên quan rõ ràng → RELEVANT.
              + Không có RELEVANT nhưng có mục liên quan một phần → PARTIAL.
              + Tất cả đều không liên quan → NOT_RELEVANT.
            - relevance LUÔN phải được trả về; không được null, rỗng hoặc giá trị khác 3 giá trị trên.
                       
            EDUCATION:
            - Chỉ trích xuất trường, bằng cấp, chuyên ngành, thời gian... nếu CV có.
            - Không tự suy đoán chuyên ngành hoặc bằng cấp.
                       
            PROJECT:
            - Chỉ tạo PROJECT khi CV rõ ràng mô tả một dự án/đề án cụ thể.
            - Không biến mọi công việc, responsibility hoặc task thành PROJECT.
            - Chỉ trích xuất thông tin có trong CV.
                       

            LỖI FORMAT:
            - Có thể ghép thông tin bị tách dòng hoặc sai thứ tự nếu ngữ cảnh cho thấy
              chắc chắn chúng thuộc cùng một mục.
            - Không ghép nếu không đủ căn cứ.
            - Không tạo bản ghi trùng do CV bị lặp nội dung.

            OUTPUT:
            - Chỉ trả về JSON đúng response schema.
            - Không markdown, không code fence, không giải thích, không text ngoài schema.
            - Tuân thủ chính xác field name, type, enum và cấu trúc của schema.
            """;

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "summary", Map.of("type", "STRING"),
                    "totalExperienceYears", Map.of("type", "NUMBER"),
//                    "skills", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                    "skills", Map.of(
                            "type", "ARRAY",
                            "items", Map.of(
                                    "type", "OBJECT",
                                    "properties", Map.of(
                                            "name", Map.of("type", "STRING"),
                                            "yearsOfExperience", Map.of("type", "NUMBER"),
                                            "level", Map.of(
                                                    "type", "STRING",
                                                    "enum", List.of(
                                                            "BASIC",
                                                            "INTERMEDIATE",
                                                            "ADVANCED",
                                                            "EXPERT"
                                                    )
                                            )
                                    ),
                                    "required", List.of("name")
                            )
                    ),
                    "experience", Map.of("type", "ARRAY", "items", Map.of(
                            "type", "OBJECT",
                            "properties", Map.of(
                                    "company", Map.of("type", "STRING"),
                                    "title", Map.of("type", "STRING"),
                                    "startDate", Map.of("type", "STRING"),
                                    "endDate", Map.of("type", "STRING"),
                                    "isCurrent", Map.of("type", "BOOLEAN"),
                                    "description", Map.of("type", "STRING")
                            )
                    )),
                    "education", Map.of("type", "ARRAY", "items", Map.of(
                            "type", "OBJECT",
                            "properties", Map.of(
                                    "school", Map.of("type", "STRING"),
                                    "degree", Map.of("type", "STRING"),
                                    "major", Map.of("type", "STRING"),
                                    "startDate", Map.of("type", "STRING"),
                                    "endDate", Map.of("type", "STRING"),
                                    "gpa", Map.of("type", "NUMBER"),
                                    "relevance", Map.of("type", "STRING")
                            )
                    )),
                    "projects", Map.of("type", "ARRAY", "items", Map.of(
                            "type", "OBJECT",
                            "properties", Map.of(
                                    "name", Map.of("type", "STRING"),
                                    "description", Map.of("type", "STRING"),
                                    "techStack", Map.of("type", "STRING"),
                                    "role", Map.of("type", "STRING"),
                                    "relevance", Map.of("type", "STRING")
                            )
                    )),
                    "certifications", Map.of("type", "ARRAY", "items", Map.of(
                            "type", "OBJECT",
                            "properties", Map.of(
                                    "name", Map.of("type", "STRING"),
                                    "issuer", Map.of("type", "STRING"),
                                    "issueDate", Map.of("type", "STRING"),
                                    "expireDate", Map.of("type", "STRING")
                            )
                    ))
            ),
            "required", List.of("summary", "totalExperienceYears", "skills", "experience", "education", "projects", "certifications")
    );


    public CandidateProfileAiResult parse(String rawCvText) {

        log.info("Starting CV parsing via LLM. rawCvText length={}", rawCvText != null ? rawCvText.length() : 0);

        LLMGeminiClient.LLMGeminiResult geminiResult = llmGeminiClient.callWithFallback(SYSTEM_INSTRUCTION, rawCvText, RESPONSE_SCHEMA);
        JsonNode json = geminiResult.json();

        // Lưu ý: Đảm bảo class của bạn đã có annotation @Slf4j ở đầu class

        List<ParsedSkill> skills = mapList(json.path("skills"), n -> {
            String name = text(n, "name");

            if (name == null || name.isBlank()) {
                return null;
            }
            ParsedSkill skill = new ParsedSkill();
            skill.setName(name);

            JsonNode yearsNode = n.path("yearsOfExperience");
            if (!yearsNode.isMissingNode() && !yearsNode.isNull()) {
                skill.setYearsOfExperience(yearsNode.asDouble());
            }

            JsonNode levelNode = n.path("level");
            if (!levelNode.isMissingNode() && !levelNode.isNull()) {
                String level = levelNode.asText(null);

                if (level != null && !level.isBlank()) {
                    try {
                        skill.setLevel(UtilFormat.parseEnumSafe(SkillLevel.class,level));
                    } catch (IllegalArgumentException e) {
                        log.warn("Không tìm thấy SkillLevel hợp lệ cho giá trị: '{}', đã gán bằng null", level);
                        skill.setLevel(null);
                    }
                }
            }
            return skill;
        });
        // Lọc bỏ các phần tử null (nếu có từ bước mapList)
        List<ParsedSkill> validSkills = skills.stream()
                .filter(Objects::nonNull)
                .toList();

        log.info("Parsed thành công tổng số {} kỹ năng:", validSkills.size());
        validSkills.forEach(skill ->
                log.info(" - Tên: {}, Số năm kinh nghiệm: {}, Level: {}",
                        skill.getName(),
                        skill.getYearsOfExperience(),
                        skill.getLevel())
        );


        CandidateProfileDetails details = CandidateProfileDetails.builder()
                .experience(mapList(json.path("experience"), n -> {
                    LocalDate start = date(n, "startDate");
                    LocalDate end = date(n, "endDate");
                    boolean isCurrent = n.path("isCurrent").asBoolean(false);
                    return CandidateProfileDetails.Experience.builder()
                            .company(text(n, "company"))
                            .title(text(n, "title"))
                            .startDate(start)
                            .endDate(end)
                            .isCurrent(isCurrent)
                            .description(text(n, "description"))
                            .durationMonths(computeDurationMonths(start, end, isCurrent))
                            .build();
                }))
                .education(mapList(json.path("education"), n -> CandidateProfileDetails.Education.builder()
                        .school(text(n, "school"))
                        .degree(text(n, "degree"))
                        .major(text(n, "major"))
                        .startDate(date(n, "startDate"))
                        .endDate(date(n, "endDate"))
                        .gpa(n.hasNonNull("gpa") ? n.path("gpa").asDouble() : null)
                        .relevance(text(n, "relevance"))
                        .build()))
                .projects(mapList(json.path("projects"), n -> CandidateProfileDetails.Project.builder()
                        .name(text(n, "name"))
                        .description(text(n, "description"))
                        .techStack(text(n, "techStack"))
                        .role(text(n, "role"))
                        .relevance(text(n, "relevance"))
                        .build()))
                .certifications(mapList(json.path("certifications"), n -> CandidateProfileDetails.Certification.builder()
                        .name(text(n, "name"))
                        .issuer(text(n, "issuer"))
                        .issueDate(date(n, "issueDate"))
                        .expireDate(date(n, "expireDate"))
                        .build()))
                .build();

        CandidateProfileAiResult result = new CandidateProfileAiResult(
                text(json, "summary"),
                computeTotalExperienceYears(details, json),
                skills,
                details,
                geminiResult.model(),
                PROMPT_VERSION
        );


        log.info("Successfully parsed CV via LLM. model={}, promptVersion={}, skillsCount={}, experienceCount={}",
                geminiResult.model(), PROMPT_VERSION, skills.size(), details.getExperience().size());

        return result;
    }

    /**
     * Không tin trực tiếp "totalExperienceYears" do AI tự cộng (AI hay cộng sai khi có
     * nhiều job chồng thời gian hoặc parse nhầm ngày). Ưu tiên TÍNH LẠI bằng Java từ
     * durationMonths của từng experience item (đã tính ở computeDurationMonths).
     * Chỉ dùng số AI trả về làm fallback khi không tính được từ ngày tháng cụ thể
     * (vd CV chỉ ghi "3 năm kinh nghiệm" mà không có ngày bắt đầu/kết thúc rõ ràng).
     */
    private Double computeTotalExperienceYears(CandidateProfileDetails details, JsonNode json) {
        int totalMonths = details.getExperience().stream()
                .map(CandidateProfileDetails.Experience::getDurationMonths)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        if (totalMonths > 0) {
            return Math.round((totalMonths / 12.0) * 10) / 10.0;
        }

        double aiValue = json.hasNonNull("totalExperienceYears") ? json.path("totalExperienceYears").asDouble() : 0.0;
        return aiValue < 0 ? 0.0 : aiValue;
    }

    /** null nếu thiếu startDate (không đủ dữ liệu để tính) - endDate null + isCurrent -> tính đến hiện tại */
    private Integer computeDurationMonths(LocalDate start, LocalDate end, boolean isCurrent) {
        if (start == null) return null;
        LocalDate effectiveEnd = end != null ? end : (isCurrent ? LocalDate.now() : null);
        if (effectiveEnd == null || effectiveEnd.isBefore(start)) return null;
        return (int) java.time.temporal.ChronoUnit.MONTHS.between(start, effectiveEnd);
    }
    private <T> List<T> mapList(JsonNode arrayNode, java.util.function.Function<JsonNode, T> mapper) {
        List<T> result = new ArrayList<>();
        arrayNode.forEach(n -> result.add(mapper.apply(n)));
        return result;
    }

    private String text(JsonNode node, String field) {
        String v = node.path(field).asText(null);
        return (v == null || v.isBlank()) ? null : v;
    }

    private LocalDate date(JsonNode node, String field) {
        String v = text(node, field);
        if (v == null) return null;
        try {
            return LocalDate.parse(v.length() >= 10 ? v.substring(0, 10) : v);
        } catch (Exception e) {
            return null; // AI thỉnh thoảng trả sai định dạng - bỏ qua thay vì fail cả CV
        }
    }
}


