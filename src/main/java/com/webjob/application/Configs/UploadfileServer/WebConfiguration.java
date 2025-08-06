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
        registry.addResourceHandler("/storage/avatar/**")
                .addResourceLocations("file:" + basePath + "/user/");
        registry.addResourceHandler("/storage/company/**")
                .addResourceLocations("file:" + basePath + "/company/");
    }

//    @Bean
//    PermissionInterceptor getPermissionInterceptor() {
//        return new PermissionInterceptor();
//    }
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        String[] paths = {"/", "/auth/login", "/auth/refresh", "/storage/**"
//                ,"/api/jobs/**","/api/companies/**","/api/skill/**","/auth/register"
//                ,"/api/subscriber/**","/resumes/by-user"
//        };
//        registry.addInterceptor(getPermissionInterceptor())
//                .excludePathPatterns(paths);
//    }
}
