package com.webjob.application.Configs.UploadfileServer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
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
        registry.addResourceHandler("/storage/avatar/**")
                .addResourceLocations("file:" + basePath + "/user/");
        registry.addResourceHandler("/storage/company/**")
                .addResourceLocations("file:" + basePath + "/company/");
    }
}
