package com.webjob.application.Configs.UploadfileServer;

import com.webjob.application.Configs.PermissionInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    @Value("${upload.base-dir}")
    private String basePath;


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/storage/resume/**")
                .addResourceLocations("file:" + basePath + "/resume/");
        registry.addResourceHandler("/storage/user/**")
                .addResourceLocations("file:" + basePath + "/user/");
        registry.addResourceHandler("/storage/company/**")
                .addResourceLocations("file:" + basePath + "/company/");
    }

    @Bean
    PermissionInterceptor getPermissionInterceptor() {
        return new PermissionInterceptor();
    }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        String[] publicEndpoints = {"/", "/api/v1/auth/login", "/api/v1/auth/refresh",
                "/api/v1/auth/register", "/storage/**", "/api/v1/jobs/**",
                "/api/v1/companies/**", "/api/v1/skills/**", "/api/v1/resumes/by-user",
                "/api/v1/subscribers/**","/api/v1/subscribers/send-mails",
                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html","/api/v1/password/**"
                ,"/login-chat"
        };
        registry.addInterceptor(getPermissionInterceptor())
                .excludePathPatterns(publicEndpoints);
    }
}
