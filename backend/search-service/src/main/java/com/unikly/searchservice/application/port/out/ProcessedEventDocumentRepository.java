package com.unikly.searchservice.application.port.out;

import com.unikly.searchservice.domain.model.ProcessedEventDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProcessedEventDocumentRepository extends ElasticsearchRepository<ProcessedEventDocument, String> {
}
