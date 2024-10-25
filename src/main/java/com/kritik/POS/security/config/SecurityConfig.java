package com.kritik.POS.security.config;

import com.kritik.POS.security.entryPoint.JWTEntryPoint;
import com.kritik.POS.security.filter.JwtFilter;
import com.kritik.POS.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static com.kritik.POS.user.model.enums.UserRole.STAFF;
import static com.kritik.POS.user.model.enums.UserRole.STORE_OWNER;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    private final UserService userService;

    @Autowired
    public SecurityConfig(UserService userService) {
        this.userService = userService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {

        httpSecurity
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/store/**").hasAnyAuthority(STAFF.name(),STORE_OWNER.name())
                        .requestMatchers("/store-owner/**").hasAnyAuthority(STORE_OWNER.name())
                        .anyRequest().permitAll())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptionHandlingConfigurer ->
                        exceptionHandlingConfigurer.authenticationEntryPoint(entryPoint())) // Custom entry point
                .addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter.class); // Adding JWT filter

        return httpSecurity.build();
    }

    @Bean
    WebSecurityCustomizer webSecurityCustomizer() {
        return web -> {
            web.ignoring().requestMatchers("/test/**", "/pre-auth/**");
        };
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void configurePathMatch(PathMatchConfigurer configurer) {
                WebMvcConfigurer.super.configurePathMatch(configurer);
            }

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS","PATCH")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }

    JWTEntryPoint entryPoint() {
        return new JWTEntryPoint();
    }

    JwtFilter jwtFilter() {
        return new JwtFilter(
                userService,
                entryPoint()
        );
    }
}
