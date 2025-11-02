package io.github.sijiezhong.track.config;

import io.github.sijiezhong.track.config.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@Profile("secure")
@EnableMethodSecurity
public class SecureProfileSecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecureProfileSecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain secureSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .headers(headers -> headers.httpStrictTransportSecurity(hsts ->
                        hsts.includeSubDomains(true).preload(true).maxAgeInSeconds(31536000)))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> {
                    // 认证失败返回401（包括没有认证信息的情况）
                    e.authenticationEntryPoint((req, res, ex) -> {
                        res.setStatus(401);
                        res.setContentType("application/json;charset=UTF-8");
                        res.getWriter().write("{\"code\":401,\"message\":\"认证失败\",\"errorCode\":\"UNAUTHORIZED\"}");
                    });
                    // 权限不足返回403（只有已认证但权限不足时才返回403）
                    e.accessDeniedHandler((req, res, ex) -> {
                        // 检查是否是因为没有认证信息导致的访问拒绝
                        // 如果是，返回401而不是403
                        org.springframework.security.core.context.SecurityContext context = 
                            org.springframework.security.core.context.SecurityContextHolder.getContext();
                        if (context.getAuthentication() == null || 
                            !context.getAuthentication().isAuthenticated() ||
                            context.getAuthentication() instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
                            res.setStatus(401);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"code\":401,\"message\":\"认证失败\",\"errorCode\":\"UNAUTHORIZED\"}");
                        } else {
                            res.setStatus(403);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"code\":403,\"message\":\"访问被拒绝\",\"errorCode\":\"FORBIDDEN\"}");
                        }
                    });
                });
        return http.build();
    }
}


