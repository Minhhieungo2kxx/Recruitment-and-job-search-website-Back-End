package com.webjob.application.enums;

import java.util.Set;

public enum IndustryCategories {
    SALES(
            "Kinh doanh / Bán hàng",
            "kinh doanh", "bán hàng", "sales"
    ),

    MARKETING(
            "Marketing / PR / Quảng cáo",
            "marketing", "pr", "quảng cáo", "truyền thông"
    ),

    IT(
            "Công nghệ thông tin / Phần mềm",
            "it", "công nghệ", "công nghệ thông tin",
            "phần mềm", "software", "technology", "tech"
    ),

    ADMIN(
            "Hành chính / Thư ký / Pháp chế",
            "hành chính", "thư ký", "pháp chế"
    ),

    ACCOUNTING(
            "Kế toán / Kiểm toán / Thuế",
            "kế toán", "kiểm toán", "thuế"
    ),

    FINANCE(
            "Tài chính / Đầu tư / Ngân hàng",
            "tài chính", "đầu tư", "ngân hàng", "banking"
    ),

    CUSTOMER_SERVICE(
            "Chăm sóc khách hàng / Vận hành",
            "chăm sóc khách hàng", "customer service", "vận hành"
    ),

    HR(
            "Nhân sự / Tuyển dụng / Đào tạo",
            "nhân sự",
            "tuyển dụng",
            "hr",
            "human resources",
            "recruitment",
            "đào tạo nhân sự",
            "đào tạo nhân viên"
    ),

    EDUCATION(
            "Giáo dục / Đào tạo",
            "giáo dục",
            "trường học",
            "education",
            "school",
            "đại học",
            "cao đẳng",
            "trung tâm đào tạo"
    ),

    RETAIL(
            "Bán lẻ / Tiêu dùng nhanh (FMCG)",
            "bán lẻ", "fmcg", "tiêu dùng"
    ),

    LOGISTICS(
            "Logistics / Vận tải / Kho bãi",
            "logistics", "vận tải", "kho bãi"
    ),

    MANUFACTURING(
            "Sản xuất / Quy trình công nghiệp",
            "sản xuất", "công nghiệp", "quy trình công nghiệp"
    ),

    ENGINEERING(
            "Cơ khí / Điện / Điện tử",
            "cơ khí", "điện", "điện tử"
    ),

    REAL_ESTATE(
            "Bất động sản",
            "bất động sản", "real estate"
    ),

    CONSTRUCTION(
            "Xây dựng / Kiến trúc",
            "xây dựng", "kiến trúc"
    ),


    HEALTHCARE(
            "Y tế / Dược phẩm / Sức khỏe",
            "y tế", "dược", "dược phẩm", "sức khỏe", "healthcare"
    ),

    HOSPITALITY(
            "Nhà hàng / Khách sạn / Du lịch",
            "nhà hàng", "khách sạn", "du lịch", "hospitality"
    ),

    DESIGN(
            "Thiết kế đồ họa / Thiết kế nội thất tổng hợp",
            "thiết kế", "thiết kế đồ họa", "thiết kế nội thất"
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

