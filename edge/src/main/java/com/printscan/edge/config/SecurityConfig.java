package com.printscan.edge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import lombok.Getter;
import lombok.Setter;

/**
 * 온디바이스 인증. HTTP Basic 으로 UI/API 전체 보호(현장 LAN 단말). 헬스·정적·h2콘솔(off)만 개방.
 * 자격증명은 printscan.security.* (env 로 반드시 교체). 기본값은 부트 편의용이며 배포 시 변경 강제.
 */
@Configuration
public class SecurityConfig {

    @Getter
    @Setter
    public static class SecurityProperties {
        private String username = "admin";
        private String password = "printscan"; // ⚠️ 배포 시 env(PRINTSCAN_SECURITY_PASSWORD)로 교체
    }

    @Bean
    @ConfigurationProperties(prefix = "printscan.security")
    SecurityProperties securityProperties() { return new SecurityProperties(); }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService users(SecurityProperties props, PasswordEncoder enc) {
        return new InMemoryUserDetailsManager(User.withUsername(props.getUsername())
                .password(enc.encode(props.getPassword())).roles("OPERATOR").build());
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(c -> c.disable()) // Basic + 상태없는 API
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/design/**", "/js/**", "/favicon.ico").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated())
            .httpBasic(b -> {});
        return http.build();
    }
}
