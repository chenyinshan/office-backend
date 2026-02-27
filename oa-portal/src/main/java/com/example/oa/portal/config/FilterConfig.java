package com.example.oa.portal.config;

import com.example.oa.portal.filter.AttachmentUploadCachingFilter;
import com.example.oa.portal.filter.AuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<AuthFilter> authFilterRegistration(AuthFilter authFilter) {
        FilterRegistrationBean<AuthFilter> bean = new FilterRegistrationBean<>(authFilter);
        bean.addUrlPatterns("/api/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<AttachmentUploadCachingFilter> attachmentUploadCachingFilter() {
        FilterRegistrationBean<AttachmentUploadCachingFilter> bean = new FilterRegistrationBean<>(new AttachmentUploadCachingFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return bean;
    }
}
