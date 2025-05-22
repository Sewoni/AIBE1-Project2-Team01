package org.sunday.projectpop.project.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.sunday.projectpop.auth.jwt.JwtTokenProvider;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthTestController {

    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/test-token")
    public ResponseEntity<String> getTestToken(@RequestParam String userId) {
        Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        String token = jwtTokenProvider.generateToken(auth, List.of("USER"));
        return ResponseEntity.ok(token);
    }
}