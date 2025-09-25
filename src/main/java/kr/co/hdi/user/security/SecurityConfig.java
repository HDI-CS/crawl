package kr.co.hdi.user.security;

import kr.co.hdi.global.filter.SessionAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfig corsConfig;
    private final SessionAuthenticationFilter sessionAuthenticationFilter;

    private static final String[] WHITE_LIST = {
            "/auth/login",
            "/auth/register",
            "/auth/register-admin",
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                // cors
                .cors(cors -> cors.configurationSource(corsConfig.apiCorsConfigurationSource()))

                // 경로별 인가
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITE_LIST).permitAll()
                        .anyRequest().authenticated()
                )

                .sessionManagement(session -> session
                        .maximumSessions(10)
                        .maxSessionsPreventsLogin(false)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(401, "Unauthorized");  // 인증 실패 시 401 응답
                        })
                )

                .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);


        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}
