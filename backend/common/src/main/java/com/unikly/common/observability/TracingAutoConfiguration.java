package com.unikly.common.observability;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Auto-configuration that registers servlet filters for all
 * servlet-based microservices. Skipped automatically for the reactive gateway.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class TracingAutoConfiguration {

    @Bean
    public FilterRegistrationBean<MdcUserIdFilter> mdcUserIdFilter() {
        FilterRegistrationBean<MdcUserIdFilter> registration = new FilterRegistrationBean<>(new MdcUserIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter() {
        FilterRegistrationBean<RequestLoggingFilter> registration = new FilterRegistrationBean<>(new RequestLoggingFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
