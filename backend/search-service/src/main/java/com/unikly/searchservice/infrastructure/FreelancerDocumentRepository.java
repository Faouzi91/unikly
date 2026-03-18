package com.unikly.searchservice.infrastructure;

import com.unikly.searchservice.domain.FreelancerDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface FreelancerDocumentRepository extends ElasticsearchRepository<FreelancerDocument, String> {
}
