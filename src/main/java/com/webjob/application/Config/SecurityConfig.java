package com.webjob.application.Config;

import com.webjob.application.Config.CustomOAuth2.OAuth2LoginFailureHandler;
import com.webjob.application.Config.CustomOAuth2.OAuth2LoginSuccessHandler;
import com.webjob.application.Config.Redis.JwtBlacklistFilter;
import com.webjob.application.Exception.CustomAccessDeniedHandler;
import com.webjob.application.Service.OAuth2.CustomOAuth2UserService;
import com.webjob.application.Exception.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final PermissionAuthorizationManager permissionAuthorizationManager;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtBlacklistFilter jwtBlacklistFilter) throws Exception {

        String[] publicEndpoints = {"/", "/api/v1/auth/login", "/api/v1/auth/refresh",
                "/api/v1/auth/register", "/storage/**", "/api/v1/subscribers/send-mails",
                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/api/v1/password/**",
                "/oauth2/**", "/login/oauth2/**" // OAuth2 login Google
                , "/api/v1/payments/vnpay-return", "/login-chat", "/chat",
                "/js/**", "/css/**", "/img/**", "/ws/**", "/audio/**", "/login-success"
                , "/api/v1/payments/momo-return"
        };


        http
                .addFilterBefore(jwtBlacklistFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                                .requestMatchers(publicEndpoints).permitAll()
                                .requestMatchers(HttpMethod.GET,
                                        "/api/v1/companies/**",
                                        "/api/v1/jobs/**",
                                        "/api/v1/skills/**"
                                ).permitAll()
                                .anyRequest().access(permissionAuthorizationManager)
//                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())

//  http://localhost:8081/oauth2/authorization/google ->yeu cau de redirect den login Google
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)
                        )
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint) // 401
                        .accessDeniedHandler(customAccessDeniedHandler) // 403
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults());

        return http.build();
    }


}
///1. Spring khởi tạo theo thứ tự:
//
//Load config (@Configuration, @EnableWebSecurity, etc.)
//
//Tạo các bean (@Service, @Component, @Repository) → như AuthService
//
//Sau đó mới tạo @Controller/
