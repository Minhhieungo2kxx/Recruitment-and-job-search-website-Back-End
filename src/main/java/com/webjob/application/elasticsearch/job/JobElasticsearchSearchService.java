package com.webjob.application.elasticsearch.job;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.webjob.application.document.JobDocument;
import com.webjob.application.dto.Request.JobFilterAdminRequest;
import com.webjob.application.dto.Request.JobFilterClient;
import com.webjob.application.dto.Request.JobSearchRequestAI;
import com.webjob.application.dto.Response.ElasticsearchSearchResult;
import com.webjob.application.enums.CompanyStatus;
import com.webjob.application.enums.JobSort;
import com.webjob.application.enums.JobStatus;
import com.webjob.application.enums.PostedDateFilter;
import com.webjob.application.utils.common.UtilFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobElasticsearchSearchService {
    private static final String INDEX_NAME = "jobs";

    private final ElasticsearchClient elasticsearchClient;


    public ElasticsearchSearchResult searchClient(int page, int size, JobFilterClient request) {

        SearchResponse<JobDocument> response = null;
        try {
            response = elasticsearchClient.search(s -> {
                SearchRequest.Builder builder = s
                        .index(INDEX_NAME)
                        .from(page * size)
                        .size(size)
                        .trackTotalHits(t -> t.enabled(true))
                        .query(buildQueryClient(request));
                applySort(builder, request.getSort(), request.getKeyword());
                return builder;
            }, JobDocument.class);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        return mapToSearchResult(response);
    }

    public ElasticsearchSearchResult searchJobAI(JobSearchRequestAI request) {

        SearchResponse<JobDocument> response = null;
        try {
            response = elasticsearchClient.search(s -> {
                SearchRequest.Builder builder = s
                        .index(INDEX_NAME)
                        .size(10)
                        .trackTotalHits(t -> t.enabled(true))
                        .query(buildQuerySearchAi(request));
                applySort(builder, null, request.getKeyword());
                return builder;
            }, JobDocument.class);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        return mapToSearchResult(response);
    }

    private Query buildKeywordQuery(String keyword) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        String value = keyword.trim();

        List<Query> queries = new ArrayList<>();

        queries.add(
                Query.of(q -> q.multiMatch(m -> m
                        .query(value)
                        .fields(
                                "name^10",
                                "jobCategoryName^8"
                        )
                        .type(TextQueryType.Phrase)
                        .boost(20.0f)
                ))
        );


        queries.add(
                Query.of(q -> q.multiMatch(m -> m
                        .query(value)
                        .fields(
                                "name^10",
                                "jobCategoryName^8",
                                "requirement^2",
                                "description"
                        )
                        .operator(Operator.And)
                        .boost(10.0f)
                ))
        );


        queries.add(
                Query.of(q -> q.nested(n -> n
                        .path("skills")
                        .query(
                                Query.of(sq -> sq.match(m -> m
                                        .field("skills.skillName")
                                        .query(value)
                                        .boost(12.0f)
                                ))
                        )
                ))
        );




        queries.add(
                Query.of(q -> q.multiMatch(m -> m
                        .query(value)
                        .fields(
                                "name^3",
                                "jobCategoryName^2",
                                "skills.skillName^2"
                        )
                        .fuzziness("AUTO")
                        .operator(Operator.And)
                        .boost(1.0f)
                ))
        );

        return Query.of(q -> q.bool(b -> b
                .should(queries)
                .minimumShouldMatch("1")
        ));
    }


    private Query buildQueryClient(JobFilterClient request) {

        List<Query> must = new ArrayList<>();
        List<Query> filter = new ArrayList<>();

//        truy van theo keyword + sore + relevance

        Query keywordQuery = buildKeywordQuery(request.getKeyword());

        if (keywordQuery != null) {
            must.add(keywordQuery);
        }
//        no deleted
        filter.add(
                Query.of(q -> q.term(t -> t
                        .field("deleted")
                        .value(FieldValue.of(false))
                ))
        );



//        con thoi gian + open
        Instant now = Instant.now();

        filter.add(
                Query.of(q -> q.term(t -> t
                        .field("status")
                        .value(FieldValue.of(JobStatus.OPEN.name()))
                ))
        );

        filter.add(
                Query.of(q -> q.range(r -> r
                        .date(d -> d
                                .field("startDate")
                                .lte(now.toString())
                        )
                ))
        );

        filter.add(
                Query.of(q -> q.range(r -> r
                        .date(d -> d
                                .field("endDate")
                                .gte(now.toString())
                        )
                ))
        );
//        location
        String location = request.getLocation();
        if (location != null && !location.isBlank()) {
            filter.add(
                    Query.of(q -> q.matchPhrase(m -> m
                            .field("location")
                            .query(location)
                    ))
            );
        }
//        jobCategoryId
        if (request.getJobCategoryId() != null) {

            filter.add(
                    Query.of(q -> q.term(t -> t
                            .field("jobCategoryId")
                            .value(
                                    FieldValue.of(
                                            request.getJobCategoryId()
                                    )
                            )
                    ))
            );
        }
//        list companies
        if (request.getCompanyIds() != null && !request.getCompanyIds().isEmpty()) {
            filter.add(
                    Query.of(q -> q.terms(t -> t
                            .field("companyId")
                            .terms(v -> v.value(
                                    request.getCompanyIds()
                                            .stream()
                                            .map(FieldValue::of)
                                            .toList()
                            ))
                    ))
            );
        }
//        company active
        filter.add(
                Query.of(q -> q.term(t -> t
                        .field("companyDeleted")
                        .value(FieldValue.of(false))
                ))
        );
        filter.add(
                Query.of(q -> q.term(t -> t
                        .field("companyStatus")
                        .value(FieldValue.of(CompanyStatus.ACTIVE.name()))
                ))
        );
//        List Skill tuong ung
        if (request.getSkillIds() != null && !request.getSkillIds().isEmpty()) {
            filter.add(
                    Query.of(q -> q.nested(n -> n
                            .path("skills")
                            .query(nq -> nq.terms(t -> t
                                    .field("skills.skillId")
                                    .terms(v -> v.value(
                                            request.getSkillIds()
                                                    .stream()
                                                    .map(FieldValue::of)
                                                    .toList()
                                    ))
                            ))
                    ))
            );
        }
        //salary
        buildSalaryFilter(
                UtilFormat.asDouble(request.getMinSalary()),
                UtilFormat.asDouble(request.getMaxSalary())
                , filter
        );
//        "experienceRequired
        if (request.getExperience() != null) {

            filter.add(
                    Query.of(q -> q.range(r -> r
                            .number(n -> n
                                    .field("experienceRequired")
                                    .lte(request.getExperience().doubleValue()
                                    )
                            )
                    ))
            );
        }
//        level
        if (request.getLevels() != null && !request.getLevels().isEmpty()) {
            filter.add(
                    Query.of(q -> q.terms(t -> t
                            .field("level")
                            .terms(v -> v.value(
                                    request.getLevels()
                                            .stream()
                                            .map(FieldValue::of)
                                            .toList()
                            ))
                    )));
        }
//        workingType
        if (request.getWorkingTypes() != null && !request.getWorkingTypes().isEmpty()) {
            filter.add(
                    Query.of(q -> q.terms(t -> t
                            .field("workingType")
                            .terms(v -> v.value(
                                    request.getWorkingTypes()
                                            .stream()
                                            .map(FieldValue::of)
                                            .toList()
                            ))
                    ))
            );
        }
//        workMode
        if (request.getWorkModes() != null && !request.getWorkModes().isEmpty()) {
            filter.add(
                    Query.of(q -> q.terms(t -> t
                            .field("workMode")
                            .terms(v -> v.value(
                                    request.getWorkModes()
                                            .stream()
                                            .map(FieldValue::of)
                                            .toList()
                            ))
                    ))
            );
        }
//        createdWithin
        createdWithin(request.getPostedDate(), filter);

//       Negotiable
        if (request.getNegotiable() != null) {

            filter.add(
                    Query.of(q -> q.term(t -> t
                            .field("negotiable")
                            .value(
                                    FieldValue.of(request.getNegotiable())
                            )
                    ))
            );
        }
        return Query.of(q -> q.bool(b -> {

            if (!must.isEmpty()) {
                b.must(must);
            }

            if (!filter.isEmpty()) {
                b.filter(filter);
            }
            return b;
        }));
    }

    private Query buildQuerySearchAi(JobSearchRequestAI request) {

        List<Query> must = new ArrayList<>();

        List<Query> filter = new ArrayList<>();

//        truy van theo keyword + sore + relevance

        Query keywordQuery = buildKeywordQuery(request.getKeyword());

        if (keywordQuery != null) {
            must.add(keywordQuery);
        }

        //        con thoi gian + Job open
        Instant now = Instant.now();

        filter.add(
                Query.of(q -> q.term(t -> t
                        .field("status")
                        .value(FieldValue.of(JobStatus.OPEN.name()))
                ))
        );

        filter.add(
                Query.of(q -> q.range(r -> r
                        .date(d -> d
                                .field("startDate")
                                .lte(now.toString())
                        )
                ))
        );

        filter.add(
                Query.of(q -> q.range(r -> r
                        .date(d -> d
                                .field("endDate")
                                .gte(now.toString())
                        )
                ))
        );
//        no deleted
        filter.add(
                Query.of(q -> q.term(t -> t
                        .field("deleted")
                        .value(FieldValue.of(false))
                ))
        );


//        location
        String location = request.getLocation();
        if (location != null && !location.isBlank()) {
            filter.add(
                    Query.of(q -> q.matchPhrase(m -> m
                            .field("location")
                            .query(location)
                    ))
            );
        }

        //salary
        buildSalaryFilter(
                UtilFormat.asDouble(request.getSalaryMin()),
                null
                , filter
        );
        //        "experienceRequired
        if (request.getExperienceYears() != null) {
            filter.add(
                    Query.of(q -> q.range(r -> r
                            .number(n -> n
                                    .field("experienceRequired")
                                    .lte(request.getExperienceYears().doubleValue()
                                    )
                            )
                    ))
            );
        }
        //        level
        if (request.getLevel() != null) {
            filter.add(
                    Query.of(
                            q -> q.term(t -> t
                                    .field("level")
                                    .value(FieldValue.of(request.getLevel().name()))
                            ))
            );
        }

        //        workMode
        if (request.getWorkMode() != null) {
            filter.add(
                    Query.of(
                            q -> q.term(t -> t
                                    .field("workMode")
                                    .value(FieldValue.of(request.getWorkMode().name()))
                            ))
            );
        }
//        WorkingType
        if (request.getWorkingType() != null) {
            filter.add(
                    Query.of(q -> q.term(t -> t
                            .field("workingType")
                            .value(FieldValue.of(request.getWorkingType().name()))
                    ))
            );
        }


//        company active
        filter.add(
                Query.of(q -> q.term(t -> t
                        .field("companyDeleted")
                        .value(FieldValue.of(false))
                ))
        );
        filter.add(
                Query.of(q -> q.term(t -> t
                        .field("companyStatus")
                        .value(FieldValue.of(CompanyStatus.ACTIVE.name()))
                ))
        );
//        companyName
        String companyName = request.getCompanyName();
        if (companyName != null && !companyName.isBlank()) {
            filter.add(
                    Query.of(q -> q.matchPhrase(m -> m
                            .field("companyName")
                            .query(companyName)
                    ))
            );
        }
//        categoryName
        String categoryName = request.getCategoryName();
        if (categoryName != null && !categoryName.isBlank()) {
            filter.add(
                    Query.of(q -> q.matchPhrase(m -> m
                            .field("jobCategoryName")
                            .query(categoryName)
                    ))
            );
        }

        return Query.of(q -> q.bool(b -> {

            if (!must.isEmpty()) {
                b.must(must);
            }

            if (!filter.isEmpty()) {
                b.filter(filter);
            }
            return b;
        }));
    }

    private void buildSalaryFilter(Double minSalary, Double maxSalary, List<Query> filter) {

        if (minSalary == null && maxSalary == null) {
            return;
        }

        if (minSalary != null && maxSalary == null) {

            filter.add(
                    Query.of(q -> q.range(r -> r
                            .number(n -> n
                                    .field("salaryMax")
                                    .gte(minSalary)
                            )
                    ))
            );

            return;
        }

        if (minSalary == null) {

            filter.add(
                    Query.of(q -> q.range(r -> r
                            .number(n -> n
                                    .field("salaryMin")
                                    .lte(maxSalary)
                            )
                    ))
            );

            return;
        }

        filter.add(
                Query.of(q -> q.bool(b -> b
                        .filter(
                                Query.of(c -> c.range(r -> r
                                        .number(n -> n
                                                .field("salaryMin")
                                                .lte(maxSalary)
                                        )
                                ))
                        )
                        .filter(
                                Query.of(c -> c.range(r -> r
                                        .number(n -> n
                                                .field("salaryMax")
                                                .gte(minSalary)
                                        )
                                ))
                        )
                ))
        );
    }

    public void createdWithin(PostedDateFilter postedDateFilter, List<Query> filter) {
        if (postedDateFilter == null) {
            return;
        }

        Instant now = Instant.now();

        Instant from = switch (postedDateFilter) {
            case LAST_24_HOURS -> now.minus(24, ChronoUnit.HOURS);
            case LAST_3_DAYS -> now.minus(3, ChronoUnit.DAYS);
            case LAST_7_DAYS -> now.minus(7, ChronoUnit.DAYS);
            case LAST_30_DAYS -> now.minus(30, ChronoUnit.DAYS);
        };

        filter.add(
                Query.of(q -> q.range(r -> r
                        .date(d -> d
                                .field("createdAt")
                                .gte(from.toString())
                        )
                ))
        );
    }


    private void applySort(SearchRequest.Builder builder, JobSort sort, String Keyword) {
        // Không có sort
        if (sort == null) {

            // Có keyword → ưu tiên relevance
            if (Keyword != null && !Keyword.trim().isEmpty()) {
                builder.sort(s -> s
                        .score(sc -> sc.order(SortOrder.Desc))
                );
                builder.sort(s -> s
                        .field(f -> f
                                .field("createdAt")
                                .order(SortOrder.Desc)
                        )
                );

                return;
            }

            // Không có keyword → mới nhất
            builder.sort(s -> s
                    .field(f -> f
                            .field("createdAt")
                            .order(SortOrder.Desc)
                    )
            );

            return;
        }

        switch (sort) {

            case NEWEST -> builder.sort(s -> s
                    .field(f -> f
                            .field("createdAt")
                            .order(SortOrder.Desc)
                    )
            );

            case SALARY_HIGH -> builder.sort(s -> s
                    .field(f -> f
                            .field("salaryMax")
                            .order(SortOrder.Desc)
                    )
            );

            case SALARY_LOW -> builder.sort(s -> s
                    .field(f -> f
                            .field("salaryMin")
                            .order(SortOrder.Asc)
                    )
            );

            case MOST_VIEWED -> builder.sort(s -> s
                    .field(f -> f
                            .field("viewCount")
                            .order(SortOrder.Desc)
                    )
            );

            case LESS_COMPETITION -> builder.sort(s -> s
                    .field(f -> f
                            .field("appliedCount")
                            .order(SortOrder.Asc)
                    )
            );

            case EXPIRING_SOON -> builder.sort(s -> s
                    .field(f -> f
                            .field("endDate")
                            .order(SortOrder.Asc)
                    )
            );
        }
    }

    public ElasticsearchSearchResult mapToSearchResult(SearchResponse<JobDocument> response) {
        List<Long> ids = response.hits()
                .hits()
                .stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(JobDocument::getId)
                .toList();

        long total = 0;
        if (response.hits().total() != null) {
            total = response.hits().total().value();
        }

        return ElasticsearchSearchResult.builder()
                .ids(ids)
                .total(total)
                .build();
    }
}

