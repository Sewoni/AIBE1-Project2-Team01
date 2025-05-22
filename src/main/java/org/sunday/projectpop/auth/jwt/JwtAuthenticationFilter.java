package org.sunday.projectpop.auth.jwt;



import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String uri = req.getRequestURI();
        log.debug("🔍 JwtAuthenticationFilter 진입 - 요청 URI: {}", uri);

        String token = null;

        // 1️⃣ Authorization 헤더 우선
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        // 2️⃣ 없으면 쿠키에서 검색
        if (token == null && req.getCookies() != null) {
            for (Cookie cookie : req.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    token = URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8);
                    break;
                }
            }
        }

        // 3️⃣ 유효성 검증 및 인증 컨텍스트 설정
        if (token != null) {
            log.debug("✅ 추출된 JWT 토큰: {}", token);

            boolean valid = jwtTokenProvider.validateToken(token);
            log.debug("🔐 JWT 유효성 검사 결과: {}", valid);

            if (valid) {
                Authentication auth = jwtTokenProvider.getAuthentication(token);
                log.debug("✅ 인증 객체 생성: {}", auth);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                log.warn("❌ 유효하지 않은 JWT 토큰");
            }
        }
        log.debug("✅ 추출된 JWT 토큰: {}", token);

        chain.doFilter(req, res);
    }
}
