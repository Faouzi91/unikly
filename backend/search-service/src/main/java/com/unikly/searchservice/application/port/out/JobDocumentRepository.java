package com.unikly.searchservice.application.port.out;

import com.unikly.searchservice.domain.model.JobDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface JobDocumentRepository extends ElasticsearchRepository<JobDocument, String> {
}
