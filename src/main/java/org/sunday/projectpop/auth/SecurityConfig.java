package org.sunday.projectpop.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.sunday.projectpop.auth.jwt.JwtAuthFilter;
import org.sunday.projectpop.auth.jwt.JwtTokenProvider;
import org.sunday.projectpop.temp.user.UserAccountRepository;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;



    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {}) // ✅ CORS 허용 추가
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/projects/apply","/projects/submit").authenticated() // 로그인 필요
                        .anyRequest().permitAll()
                )

                .formLogin(form -> form    // ✅ 이 줄 추가
                          // or 제거하면 기본 제공 로그인 UI
                        .permitAll()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // ✅ 전역 CORS 설정 (모든 origin 허용)
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")   // 모든 origin 허용 (개발용)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }

//    public UserDetailsService userDetailsService() {
//        UserDetails user = User.builder()
//                .username("u01")
//                .password("encoded_pw1")
//                .roles("USER")
//                .build();
//
//        return new InMemoryUserDetailsManager(user);
//    }
    @Bean
    public UserDetailsService userDetailsService(UserAccountRepository userAccountRepository) {
        return username -> {
            return userAccountRepository.findById(username)
                    .map(user -> new org.springframework.security.core.userdetails.User(
                            user.getUserId(), user.getPassword(), List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    ))
                    .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        };
    }

    @SuppressWarnings("deprecation")
    @Bean
    public static org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance(); // ⚠️ 테스트용
    }
}
