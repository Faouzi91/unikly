package com.unikly.searchservice.application;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.json.JsonData;
import com.unikly.common.dto.PageResponse;
import com.unikly.searchservice.api.dto.FreelancerSearchResult;
import com.unikly.searchservice.api.dto.JobSearchResult;
import com.unikly.searchservice.api.exception.SearchUnavailableException;
import com.unikly.searchservice.domain.FreelancerDocument;
import com.unikly.searchservice.domain.JobDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final Set<String> KNOWN_SKILLS = Set.of(
            "Java", "Spring", "Spring Boot", "Angular", "React", "TypeScript", "JavaScript",
            "Python", "Django", "Flask", "Node.js", "Express", "Go", "Rust", "C++", "C#",
            ".NET", "Ruby", "Rails", "PHP", "Laravel", "Swift", "Kotlin", "Flutter", "Dart",
            "Docker", "Kubernetes", "AWS", "Azure", "GCP", "PostgreSQL", "MySQL", "MongoDB",
            "Redis", "Elasticsearch", "Kafka", "RabbitMQ", "GraphQL", "REST", "gRPC",
            "Machine Learning", "Deep Learning", "TensorFlow", "PyTorch", "Data Science",
            "DevOps", "CI/CD", "Git", "Linux", "Terraform", "Ansible",
            "HTML", "CSS", "Tailwind CSS", "SASS", "SCSS", "Bootstrap",
            "Figma", "UI/UX", "Photoshop", "Illustrator",
            "Blockchain", "Solidity", "Web3", "Smart Contracts"
    );

    private final ElasticsearchOperations elasticsearchOperations;

    public PageResponse<JobSearchResult> searchJobs(String query, List<String> skills,
                                                     Double minBudget, Double maxBudget,
                                                     int page, int size) {
        var boolBuilder = new BoolQuery.Builder();

        // Always filter to OPEN jobs
        boolBuilder.filter(TermQuery.of(t -> t.field("status").value("OPEN"))._toQuery());

        // Full-text search on title + description
        if (query != null && !query.isBlank()) {
            boolBuilder.must(MultiMatchQuery.of(m -> m
                    .fields("title", "description")
                    .query(query))._toQuery());
        }

        // Filter by skills
        if (skills != null && !skills.isEmpty()) {
            var fieldValues = skills.stream()
                    .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                    .toList();
            boolBuilder.filter(TermsQuery.of(t -> t
                    .field("skills")
                    .terms(TermsQueryField.of(tf -> tf.value(fieldValues))))._toQuery());
        }

        // Budget range filter
        if (minBudget != null || maxBudget != null) {
            boolBuilder.filter(RangeQuery.of(r -> {
                var range = r.number(n -> {
                    var nb = n.field("budget");
                    if (minBudget != null) nb.gte(minBudget);
                    if (maxBudget != null) nb.lte(maxBudget);
                    return nb;
                });
                return range;
            })._toQuery());
        }

        var nativeQuery = NativeQuery.builder()
                .withQuery(boolBuilder.build()._toQuery())
                .withPageable(PageRequest.of(page, size))
                .withSort(s -> s.score(sc -> sc))
                .withSort(s -> s.field(f -> f.field("createdAt").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)))
                .build();

        try {
            var hits = elasticsearchOperations.search(nativeQuery, JobDocument.class);
            var results = hits.getSearchHits().stream()
                    .map(hit -> toJobSearchResult(hit.getContent(), hit.getScore()))
                    .toList();
            long totalHits = hits.getTotalHits();
            int totalPages = (int) Math.ceil((double) totalHits / size);
            return new PageResponse<>(results, page, size, totalHits, totalPages);
        } catch (Exception e) {
            log.error("Elasticsearch job search failed", e);
            throw new SearchUnavailableException("Search temporarily unavailable");
        }
    }

    public PageResponse<FreelancerSearchResult> searchFreelancers(String query, List<String> skills,
                                                                    Double minRating,
                                                                    int page, int size) {
        var boolBuilder = new BoolQuery.Builder();

        // Full-text search on displayName + bio
        if (query != null && !query.isBlank()) {
            boolBuilder.must(MultiMatchQuery.of(m -> m
                    .fields("displayName", "bio", "skills")
                    .query(query))._toQuery());
        }

        // Filter by skills
        if (skills != null && !skills.isEmpty()) {
            var fieldValues = skills.stream()
                    .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                    .toList();
            boolBuilder.filter(TermsQuery.of(t -> t
                    .field("skills")
                    .terms(TermsQueryField.of(tf -> tf.value(fieldValues))))._toQuery());
        }

        // Minimum rating filter
        if (minRating != null) {
            boolBuilder.filter(RangeQuery.of(r -> r
                    .number(n -> n.field("averageRating").gte(minRating)))._toQuery());
        }

        var nativeQuery = NativeQuery.builder()
                .withQuery(boolBuilder.build()._toQuery())
                .withPageable(PageRequest.of(page, size))
                .withSort(s -> s.score(sc -> sc))
                .withSort(s -> s.field(f -> f.field("averageRating").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)))
                .build();

        try {
            var hits = elasticsearchOperations.search(nativeQuery, FreelancerDocument.class);
            var results = hits.getSearchHits().stream()
                    .map(hit -> toFreelancerSearchResult(hit.getContent(), hit.getScore()))
                    .toList();
            long totalHits = hits.getTotalHits();
            int totalPages = (int) Math.ceil((double) totalHits / size);
            return new PageResponse<>(results, page, size, totalHits, totalPages);
        } catch (Exception e) {
            log.error("Elasticsearch freelancer search failed", e);
            throw new SearchUnavailableException("Search temporarily unavailable");
        }
    }

    public List<String> getSuggestions(String prefix, String type) {
        if (prefix == null || prefix.isBlank()) {
            return List.of();
        }

        if ("skill".equalsIgnoreCase(type)) {
            var lowerPrefix = prefix.toLowerCase();
            return KNOWN_SKILLS.stream()
                    .filter(skill -> skill.toLowerCase().startsWith(lowerPrefix))
                    .sorted()
                    .limit(10)
                    .toList();
        }

        return List.of();
    }

    private JobSearchResult toJobSearchResult(JobDocument doc, float score) {
        return new JobSearchResult(
                doc.getJobId(), doc.getTitle(), doc.getDescription(),
                doc.getSkills(), doc.getBudget(), doc.getCurrency(),
                doc.getStatus(), doc.getClientId(), doc.getCreatedAt(), score
        );
    }

    private FreelancerSearchResult toFreelancerSearchResult(FreelancerDocument doc, float score) {
        return new FreelancerSearchResult(
                doc.getUserId(), doc.getDisplayName(), doc.getSkills(),
                doc.getHourlyRate(), doc.getAverageRating(), doc.getLocation(),
                doc.getBio(), score
        );
    }
}
