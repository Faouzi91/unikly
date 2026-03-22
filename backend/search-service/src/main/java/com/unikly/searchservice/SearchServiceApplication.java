package com.unikly.searchservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.unikly.searchservice",
        "com.unikly.common.events",
        "com.unikly.common.dto",
        "com.unikly.common.jackson",
        "com.unikly.common.observability"
})
public class SearchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchServiceApplication.class, args);
    }
}
