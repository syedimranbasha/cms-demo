package com.walmart.cms.security.config;

import com.walmart.cms.security.principal.CmsPrincipalArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class CmsWebMvcConfiguration implements WebMvcConfigurer {

    private final CmsPrincipalArgumentResolver principalArgumentResolver;

    public CmsWebMvcConfiguration(
            CmsPrincipalArgumentResolver principalArgumentResolver) {
        this.principalArgumentResolver = principalArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(
            List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(principalArgumentResolver);
    }
}