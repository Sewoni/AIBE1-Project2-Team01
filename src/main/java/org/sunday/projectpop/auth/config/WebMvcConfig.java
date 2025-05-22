package org.sunday.projectpop.auth.config;


import lombok.RequiredArgsConstructor;
import org.sunday.projectpop.auth.jwt.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**") // 모든 경로에 적용
                .excludePathPatterns("/auth/test-token",        // ✅ 예외 처리
                        "/auth/**",                // 또는 전체 인증 경로
                        "/public/**"); // 인증 없이 허용할 경로
    }
}
