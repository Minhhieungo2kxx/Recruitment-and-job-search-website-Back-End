package com.webjob.application.enums;

import java.util.Set;

public enum IndustryCategories {
    SALES(
            "Kinh doanh / Bán hàng",
            "kinh doanh", "bán hàng", "sales", "business development"
    ),

    MARKETING(
            "Marketing / PR / Quảng cáo",
            "marketing", "pr", "quảng cáo", "truyền thông", "brand"
    ),

    IT(
            "Công nghệ thông tin / Phần mềm",
            "it", "công nghệ", "công nghệ thông tin",
            "phần mềm", "software", "technology", "tech", "developer"
    ),

    ADMIN(
            "Hành chính / Thư ký",
            "hành chính", "thư ký", "văn thư", "assistant"
    ),

    LEGAL(
            "Pháp chế / Luật",
            "pháp chế", "luật", "legal", "luật sư"
    ),

    ACCOUNTING(
            "Kế toán / Kiểm toán / Thuế",
            "kế toán", "kiểm toán", "thuế", "accounting", "auditing"
    ),

    FINANCE(
            "Tài chính / Đầu tư / Ngân hàng",
            "tài chính", "đầu tư", "ngân hàng", "banking", "finance"
    ),

    CUSTOMER_SERVICE(
            "Chăm sóc khách hàng / Vận hành",
            "chăm sóc khách hàng", "customer service", "vận hành", "operations", "cskh"
    ),

    HR(
            "Nhân sự / Tuyển dụng / Đào tạo",
            "nhân sự", "tuyển dụng", "hr", "human resources",
            "recruitment", "đào tạo nhân sự", "đào tạo nhân viên"
    ),

    EDUCATION(
            "Giáo dục / Đào tạo",
            "giáo dục", "trường học", "education",
            "school", "đại học", "cao đẳng", "trung tâm đào tạo", "giảng viên", "teacher"
    ),

    RETAIL(
            "Bán lẻ / Tiêu dùng nhanh (FMCG)",
            "bán lẻ", "fmcg", "tiêu dùng", "retail"
    ),

    LOGISTICS(
            "Logistics / Vận tải / Kho bãi",
            "logistics", "vận tải", "kho bãi", "supply chain", "xuất nhập khẩu"
    ),

    MANUFACTURING(
            "Sản xuất / Quy trình công nghiệp",
            "sản xuất", "công nghiệp", "quy trình công nghiệp", "manufacturing"
    ),

    ENGINEERING(
            "Cơ khí / Điện / Điện tử",
            "cơ khí", "điện", "điện tử", "automation", "kỹ thuật"
    ),

    REAL_ESTATE(
            "Bất động sản",
            "bất động sản", "real estate", "bđs"
    ),

    CONSTRUCTION(
            "Xây dựng / Kiến trúc",
            "xây dựng", "kiến trúc", "construction", "architecture"
    ),

    HEALTHCARE(
            "Y tế / Dược phẩm / Sức khỏe",
            "y tế", "dược", "dược phẩm", "sức khỏe", "healthcare", "doctor", "nurse"
    ),

    HOSPITALITY(
            "Nhà hàng / Khách sạn / Du lịch",
            "nhà hàng", "khách sạn", "du lịch", "hospitality", "hotel", "f&b"
    ),

    DESIGN(
            "Thiết kế đồ họa / Nội thất",
            "thiết kế", "thiết kế đồ họa", "thiết kế nội thất", "graphic design", "ui/ux"
    );

    private final String name;
    private final Set<String> keywords;

    IndustryCategories(String name, String ... keywords) {
        this.name = name;
        this.keywords = Set.of(keywords);
    }

    public String getName() {
        return name;
    }

    public Set<String> getKeywords() {
        return keywords;
    }
}

