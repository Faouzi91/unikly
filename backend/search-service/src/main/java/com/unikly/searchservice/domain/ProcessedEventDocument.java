package com.unikly.searchservice.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Document(indexName = "processed-events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEventDocument {

    @Id
    private String eventId;

    @Field(type = FieldType.Date)
    private Instant processedAt;
}
