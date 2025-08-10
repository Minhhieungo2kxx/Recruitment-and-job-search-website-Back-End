package com.webjob.application.Configs;

import com.webjob.application.Configs.CustomOAuth2.OAuth2LoginFailureHandler;
import com.webjob.application.Configs.CustomOAuth2.OAuth2LoginSuccessHandler;
import com.webjob.application.Services.OAuth2.CustomOAuth2UserService;
import com.webjob.application.Utils.exceptions.CustomAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final CustomOAuth2UserService customOAuth2UserService;



    public SecurityConfig(CustomAuthenticationEntryPoint customAuthenticationEntryPoint, OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler, OAuth2LoginFailureHandler oAuth2LoginFailureHandler, CustomOAuth2UserService customOAuth2UserService) {
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.oAuth2LoginFailureHandler = oAuth2LoginFailureHandler;
        this.customOAuth2UserService = customOAuth2UserService;
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        String[] paths = {"/", "/auth/login", "/auth/refresh", "/storage/**"
//                ,"/api/jobs/**","/api/companies/**","/api/skill/**","/auth/register"
//                ,"/resumes/by-user"
//        };
        String[] publicEndpoints = {"/", "/api/v1/auth/login", "/api/v1/auth/refresh",
                "/api/v1/auth/register", "/storage/**","/api/v1/subscribers/send-mails",
                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html","/api/v1/password/**",
                "/oauth2/**","/login/oauth2/**" // OAuth2 login Google

        };


        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicEndpoints).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/companies/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/jobs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/skills/**").permitAll()
                        .anyRequest().authenticated()
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
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(customAuthenticationEntryPoint)

                )

                .exceptionHandling(exceptions -> exceptions

                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler()) // 403
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults());

        return http.build();
    }


}
