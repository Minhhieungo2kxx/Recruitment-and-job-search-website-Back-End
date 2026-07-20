package com.webjob.application.enums;

public enum JobSort {
    NEWEST,

    SALARY_HIGH,

    SALARY_LOW,

    MOST_VIEWED,

    LESS_COMPETITION,

    EXPIRING_SOON

//    Specification<Job> spec =
//            Specification
//                    .where(JobSpecification.activeOnly())
//                    .and(JobSpecification.createdWithin(
//                            request.getPostedDate()
//                    ));
//
//
//    Pageable pageable =
//            PageRequest.of(
//                    page,
//                    size,
//                    JobSortMapper.toSort(request.getSort())
//            );
//Với entity hiện tại của bạn, cấu trúc đã đủ để xây dựng một hệ thống tìm kiếm việc làm hoàn chỉnh theo hướng giống
// các nền tảng tuyển dụng hiện nay.
}
