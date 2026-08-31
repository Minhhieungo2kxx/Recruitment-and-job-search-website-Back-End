package com.webjob.application.elasticsearch.company;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.webjob.application.document.CompanyDocument;
import com.webjob.application.document.JobDocument;
import com.webjob.application.dto.Request.CompanySearchRequest;
import com.webjob.application.dto.Request.JobFilterClient;
import com.webjob.application.dto.Request.SearchCompanyAI;
import com.webjob.application.dto.Response.ElasticsearchSearchResult;
import com.webjob.application.enums.CompanyStatus;
import com.webjob.application.enums.IndustryCategories;
import com.webjob.application.enums.JobSort;
import com.webjob.application.enums.JobStatus;
import com.webjob.application.utils.common.UtilFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyElasticsearchSearchService {
    private final ElasticsearchClient elasticsearchClient;
    private static final String INDEX_NAME_COMPANY = "companies";

    public ElasticsearchSearchResult searchCompanyClient(int page, int size, CompanySearchRequest request ) {
        SearchResponse<CompanyDocument> response = null;
        try {
            response = elasticsearchClient.search(s -> {
                SearchRequest.Builder builder = s
                        .index(INDEX_NAME_COMPANY)
                        .from(page * size)
                        .size(size)
                        .trackTotalHits(t -> t.enabled(true))
                        .query(buildQueryClient(request));
                applySort(builder, null, request.getKeyword());
                return builder;
            }, CompanyDocument.class);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        return mapToSearchResult(response);
    }
    public ElasticsearchSearchResult searchCompanyForAi(SearchCompanyAI request) {
        SearchResponse<CompanyDocument> response = null;
        try {
            response = elasticsearchClient.search(s -> {
                SearchRequest.Builder builder = s
                        .index(INDEX_NAME_COMPANY)
                        .size(10)
                        .trackTotalHits(t -> t.enabled(true))
                        .query(buildQuerySeachAI(request));
                applySort(builder, null, request.getName());
                return builder;
            }, CompanyDocument.class);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        return mapToSearchResult(response);
    }

    private Query buildQueryClient(CompanySearchRequest request) {

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

//      status active
        filter.add(
                Query.of(q -> q.term(t -> t
                        .field("status")
                        .value(FieldValue.of(CompanyStatus.ACTIVE.name()))
                ))
        );

//   hasIndustry
        String industryName = request.getIndustry();
        if (industryName != null && !industryName.isBlank()) {
            filter.add(
                    Query.of(q -> q.term(t -> t
                            .field("industryName.keyword")
                            .value(industryName)
                    ))
            );
        }
        // hasTaxCode
        String taxCode = request.getTaxCode();
        if (taxCode != null && !taxCode.isBlank()) {
            filter.add(
                    Query.of(q -> q.term(t -> t
                            .field("taxCode")
                            .value(UtilFormat.normalizeTaxCode(taxCode))
                    ))
            );
        }
// hasEmail
        String email = request.getEmail();
        if (email != null && !email.isBlank()) {
            filter.add(
                    Query.of(q -> q.term(t -> t
                            .field("email")
                            .value(email.trim())
                    ))
            );
        }
        // has phone
        String phone = request.getPhone();
        if (phone != null && !phone.isBlank()) {
            filter.add(
                    Query.of(q -> q.term(t -> t
                            .field("phone")
                            .value(phone.trim())
                    ))
            );
        }
        // has phone
        String website = request.getWebsite();
        if (website != null && !website.isBlank()) {
            filter.add(
                    Query.of(q -> q.term(t -> t
                            .field("website")
                            .value(website.trim())
                    ))
            );
        }
//        has address
        String address=request.getAddress();
        if(address!=null && !address.isBlank()){
            filter.add(
                    Query.of(q -> q.matchPhrase(t -> t
                            .field("address")
                            .query(address.trim())
                    ))
            );
        }
//        employeeSize
        Integer minEmployeeSize=request.getMinEmployeeSize();
        if(minEmployeeSize!=null){
            filter.add(
                    Query.of(q -> q.range(r -> r
                            .number(n -> n
                                    .field("employeeSize")
                                    .gte(minEmployeeSize.doubleValue())
                            )
                    ))
            );

        }

        Integer maxEmployeeSize=request.getMaxEmployeeSize();
        if(maxEmployeeSize!=null){
            filter.add(
                    Query.of(q -> q.range(r -> r
                            .number(n -> n
                                    .field("employeeSize")
                                    .lte(maxEmployeeSize.doubleValue())
                            )
                    ))
            );

        }
//        foundedYear
        Integer foundedYearFrom=request.getFoundedFrom();
        if(foundedYearFrom!=null){
            filter.add(
                    Query.of(q -> q.range(r -> r
                            .number(n -> n
                                    .field("foundedYear")
                                    .gte(foundedYearFrom.doubleValue())
                            )
                    ))
            );

        }
        Integer foundedYearTo=request.getFoundedTo();
        if(foundedYearTo!=null){
            filter.add(
                    Query.of(q -> q.range(r -> r
                            .number(n -> n
                                    .field("foundedYear")
                                    .lte(foundedYearTo.doubleValue())
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
    private Query buildQuerySeachAI(SearchCompanyAI request) {

        List<Query> must = new ArrayList<>();

        List<Query> filter = new ArrayList<>();

//        truy van theo keyword + sore + relevance
        Query keywordQuery = buildKeywordQuery(request.getName());
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

//      status active
        filter.add(
                Query.of(q -> q.term(t -> t
                        .field("status")
                        .value(FieldValue.of(CompanyStatus.ACTIVE.name()))
                ))
        );

        // hasTaxCode
        String taxCode = request.getTaxCode();
        if (taxCode != null && !taxCode.isBlank()) {
            filter.add(
                    Query.of(q -> q.term(t -> t
                            .field("taxCode")
                            .value(UtilFormat.normalizeTaxCode(taxCode))
                    ))
            );
        }
        // hasEmail
        String email = request.getEmail();
        if (email != null && !email.isBlank()) {
            filter.add(
                    Query.of(q -> q.term(t -> t
                            .field("email")
                            .value(email.trim())
                    ))
            );
        }
        // has phone
        String phone = request.getPhone();
        if (phone != null && !phone.isBlank()) {
            filter.add(
                    Query.of(q -> q.term(t -> t
                            .field("phone")
                            .value(phone.trim())
                    ))
            );
        }
        // has website
        String website = request.getWebsite();
        if (website != null && !website.isBlank()) {
            filter.add(
                    Query.of(q -> q.term(t -> t
                            .field("website")
                            .value(website.trim())
                    ))
            );
        }
        //        has address
        String address=request.getAddress();
        if(address!=null && !address.isBlank()){
            filter.add(
                    Query.of(q -> q.matchPhrase(t -> t
                            .field("address")
                            .query(address.trim())
                    ))
            );
        }


//   hasIndustry
        String industryName = resolveIndustry(request.getIndustryName());
        if (industryName != null && !industryName.isBlank()) {
            filter.add(
                    Query.of(q -> q.term(t -> t
                            .field("industryName.keyword")
                            .value(industryName)
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



    private Query buildKeywordQuery(String keyword) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        String value = keyword.trim();

        List<Query> queries = new ArrayList<>();

        // 1. Exact company name
        queries.add(
                Query.of(q -> q.term(t -> t
                        .field("name.keyword")
                        .value(value)
                        .boost(100.0f)
                ))
        );

        // 2. Exact phrase trong company name
        queries.add(
                Query.of(q -> q.matchPhrase(mp -> mp
                        .field("name")
                        .query(value)
                        .boost(50.0f)
                ))
        );

        // 3. Phrase match trên các field khác
        queries.add(
                Query.of(q -> q.multiMatch(m -> m
                        .query(value)
                        .fields(
                                "name^10",
                                "industryName^6",
                                "address^3",
                                "description^2"
                        )
                        .type(TextQueryType.Phrase)
                        .boost(20.0f)

                ))
        );

        // 4. AND - tất cả token phải match
        queries.add(
                Query.of(q -> q.multiMatch(m -> m
                        .query(value)
                        .fields(
                                "name^10",
                                "industryName^6",
                                "address^3",
                                "description^2"
                        )
                        .operator(Operator.And)
                        .boost(10.0f)
                ))
        );

        // 5. Exact keyword cho các field keyword
        queries.add(
                Query.of(q -> q.term(t -> t
                        .field("industryName.keyword")
                        .value(value)
                        .boost(15.0f)
                ))
        );

        queries.add(
                Query.of(q -> q.term(t -> t
                        .field("address.keyword")
                        .value(value)
                        .boost(10.0f)
                ))
        );

        // 6. Flexible match - điểm thấp
        queries.add(
                Query.of(q -> q.multiMatch(m -> m
                        .query(value)
                        .fields(
                                "name^8",
                                "industryName^5",
                                "address^3",
                                "description"
                        )
                        .operator(Operator.Or)
                        .minimumShouldMatch("60%")
                        .boost(3.0f)
                ))
        );

        // 7. Fuzzy fallback - điểm rất thấp
        queries.add(
                Query.of(q -> q.multiMatch(m -> m
                        .query(value)
                        .fields(
                                "name^8",
                                "industryName^5",
                                "address^2"
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


    private void applySort(SearchRequest.Builder builder, JobSort sort, String Keyword) {
        // Không có sort
        if (sort == null) {

            // Có keyword → ưu tiên relevance
            if (Keyword != null && !Keyword.trim().isEmpty()) {
                builder.sort(s -> s.score(sc -> sc.
                        order(SortOrder.Desc))
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

    }
    public ElasticsearchSearchResult mapToSearchResult(SearchResponse<CompanyDocument> response) {
        List<Long> ids = response.hits()
                .hits()
                .stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(CompanyDocument::getId)
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
    public  String resolveIndustry(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String value = input.trim().toLowerCase();

        for (IndustryCategories industry : IndustryCategories.values()) {
            if (industry.getKeywords().stream()
                    .anyMatch(value::contains)) {
                return industry.getName();
            }
        }
        return null;
    }

}
