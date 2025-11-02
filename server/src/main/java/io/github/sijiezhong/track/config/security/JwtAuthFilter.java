package io.github.sijiezhong.track.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            // 简化：token 形如 "role:ADMIN" / "role:ANALYST" / "role:DEVELOPER" / "role:READONLY"
            if (token.startsWith("role:")) {
                String role = token.substring("role:".length()).trim();
                List<GrantedAuthority> auths = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
                var authentication = new UsernamePasswordAuthenticationToken("user", null, auths);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        // 如果没有认证信息且路径需要认证（通过@PreAuthorize），Security框架会自动处理
        // 这里不抛出异常，让Security框架的authenticationEntryPoint处理
        filterChain.doFilter(request, response);
    }
}


