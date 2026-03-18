package com.unikly.searchservice.infrastructure;

import com.unikly.searchservice.domain.JobDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface JobDocumentRepository extends ElasticsearchRepository<JobDocument, String> {
}
