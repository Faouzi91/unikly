package com.unikly.searchservice.application.port.out;

import com.unikly.searchservice.domain.model.FreelancerDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface FreelancerDocumentRepository extends ElasticsearchRepository<FreelancerDocument, String> {
}
