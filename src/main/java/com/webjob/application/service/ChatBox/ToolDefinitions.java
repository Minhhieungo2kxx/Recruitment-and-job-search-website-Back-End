package com.webjob.application.service.ChatBox;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
public class ToolDefinitions {

    public Map<String, Object> buildToolsPayload() {
        return Map.of(
                "function_declarations", List.of(
                        function(
                                "searchJobs",
                                "Tìm kiếm công việc theo kỹ năng, tên công việc, địa điểm, mức lương, cấp bậc, kinh nghiệm, hình thức làm việc, loại hình công việc, công ty và danh mục nghề.",
                                Map.of(
                                        "keyword", param(
                                                "string",
                                                "Tên công việc hoặc kỹ năng. Ví dụ: Java, Spring Boot, Backend, Data Analyst"
                                        ),

                                        "location", param(
                                                "string",
                                                "Địa điểm làm việc. Ví dụ: Hà Nội, TP.HCM, Đà Nẵng"
                                        ),

                                        "salaryMin", param(
                                                "number",
                                                "Mức lương tối thiểu mong muốn (VND)"
                                        ),

                                        "experienceYears", param(
                                                "integer",
                                                "Số năm kinh nghiệm của ứng viên"
                                        ),

                                        "level", param(
                                                "string",
                                                "Cấp bậc công việc: INTERN, FRESHER, JUNIOR, MIDDLE, SENIOR, LEAD, MANAGER, DIRECTOR"
                                        ),
                                        "workMode", param(
                                                "string",
                                                "Hình thức làm việc: OFFICE, REMOTE, HYBRID"
                                        ),

                                        "workingType", param(
                                                "string",
                                                "Loại hình công việc: FULL_TIME, PART_TIME, INTERNSHIP, CONTRACT, FREELANCE"
                                        ),

                                        "company", param(
                                                "string",
                                                "Tên công ty nếu người dùng muốn tìm việc tại một công ty cụ thể"
                                        ),

                                        "category", param(
                                                "string",
                                                "Danh mục nghề nghiệp. Ví dụ: Information Technology, Backend Development, Frontend Development, Fullstack Development, Mobile Development, AI & Data Science, DevOps & Cloud Engineering, Marketing & Communications, Finance & Investment, Human Resources Management"
                                        )
                                ),
                                List.of()
                        ),

                        function(
                                "getJobDetail",
                                "Lấy thông tin chi tiết của một công việc theo jobId. Dùng khi user hỏi sâu về một job cụ thể như mô tả, yêu cầu, kỹ năng, lương, phúc lợi, công ty, địa điểm. Không dùng khi user cần tìm kiếm nhiều job.",
                                Map.of(
                                        "jobId",
                                        param("integer", "ID của công việc")
                                ),
                                List.of("jobId")
                        ),
                        function(
                                "searchCompany",
                                "Tìm công ty theo tên, MST, email, SĐT, website, địa chỉ hoặc lĩnh vực. Trả về thông tin chi tiết gồm mã số thuế và các thông tin liên quan.",
                                Map.of(
                                        "name", param("string", "Tên công ty"),
                                        "taxCode", param("string", "Mã số thuế"),
                                        "email", param("string", "Email"),
                                        "phone", param("string", "Số điện thoại"),
                                        "website", param("string", "Website"),
                                        "address", param("string", "Địa chỉ hoặc khu vực của công ty (ví dụ: Hà Nội, TP.HCM, Đà Nẵng)"),
                                        "industry", param("string", "Lĩnh vực hoặc ngành nghề kinh doanh của công ty (ví dụ: Công nghệ thông tin, Tài chính, Giáo dục)")
                                ),
                                List.of()
                        ),
                        function(
                                "getApplicationSummary",
                                "Lấy thống kê tổng quan về các công việc mà user hiện tại đã ứng tuyển, bao gồm tổng số đơn ứng tuyển và số lượng theo từng trạng thái xử lý.",
                                Map.of(),
                                List.of()
                        ),
                        function(
                                "getAppliedJobs",
                                "Lấy danh sách các công việc mà người dùng hiện tại đã ứng tuyển. Hỗ trợ lọc theo trạng thái, tìm kiếm theo tên công việc hoặc công ty và giới hạn số lượng kết quả. Chỉ sử dụng các tham số mà người dùng yêu cầu.",
                                Map.of(
                                        "status", param(
                                                "string",
                                                "Trạng thái hồ sơ ứng tuyển (PENDING, REVIEWING, INTERVIEW, ACCEPTED, REJECTED)"
                                        ),
                                        "keyword", param(
                                                "string",
                                                "Tên công việc hoặc tên công ty cần tìm kiếm"
                                        ),
                                        "limit", param(
                                                "integer",
                                                "Số lượng kết quả tối đa cần trả về.Mặc định là 10 nếu không truyền"
                                        )
                                ),
                                List.of()
                        ),
                        function(
                                "saveFavoriteJob",
                                "Lưu một công việc vào danh sách yêu thích của user hiện tại.",
                                Map.of("jobId", param("integer", "ID công việc cần lưu")),
                                List.of("jobId")
                        ),
                        function(
                                "removeFavoriteJob",
                                "Xóa một công việc khỏi danh sách yêu thích của người dùng hiện tại.",
                                Map.of("jobId", param("integer", "ID của công việc cần xóa khỏi danh sách yêu thích.")
                                ),
                                List.of("jobId")
                        ),
                        function(
                                "getFavoriteJobs",
                                """
                                Lấy danh sách các công việc mà người dùng đã lưu hoặc đánh dấu yêu thích.
                            
                                Khi trả lời người dùng, với mỗi công việc hãy hiển thị:
                                - Tiêu đề công việc
                                - Công ty
                                - Mức lương
                                - Địa điểm
                                - Loại hình làm việc
                                - Hạn nộp hồ sơ
                                """,
                                Map.of(
                                        "limit", param("integer", "Số lượng kết quả tối đa cần trả về. Mặc định là 10 nếu không truyền.")
                                ),
                                List.of()
                        ),
                        function(
                                "findMatchingJobs",
                                "Vai trò: AI Job Matcher.\n" +
                                        "Nhiệm vụ: Dựa vào SubscriberSkill và Job Alert, trả về tối đa 10 job phù hợp nhất, sắp xếp theo độ tương thích giảm dần.\n" +
                                        "Yêu cầu:\n" +
                                        "- Giải thích lý do phù hợp (nếu score thấp, nêu rõ nguyên nhân).\n" +
                                        "- KHÔNG đề xuất job ngoài danh sách Job Alert.\n" +
                                        "- Nếu thiếu dữ liệu/Job Alert, yêu cầu người dùng cập nhật.",
                                Map.of(),
                                List.of()
                        )





                )
        );
    }

    public Map<String, Object> function(String name, String description, Map<String, Object> properties, List<String> required) {
        return Map.of(
                "name", name,
                "description", description,
                "parameters", Map.of(
                        "type", "object",
                        "properties", properties,
                        "required", required
                )
        );
    }

    private  Map<String, Object> param(String type, String description) {
        return Map.of("type", type, "description", description);
    }
}
