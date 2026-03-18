package com.unikly.searchservice.infrastructure;

import com.unikly.searchservice.domain.ProcessedEventDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProcessedEventDocumentRepository extends ElasticsearchRepository<ProcessedEventDocument, String> {
}
