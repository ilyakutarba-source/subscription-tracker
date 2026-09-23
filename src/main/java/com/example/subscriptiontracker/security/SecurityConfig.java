package com.example.subscriptiontracker.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            RestAuthenticationEntryPoint authenticationEntryPoint,
                                            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        RequestMatcher apiRequest = request -> request.getRequestURI()
                .substring(request.getContextPath().length())
                .startsWith("/api/");
        LoginUrlAuthenticationEntryPoint webAuthenticationEntryPoint =
                new LoginUrlAuthenticationEntryPoint("/login");
        AccessDeniedHandlerImpl webAccessDeniedHandler = new AccessDeniedHandlerImpl();
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/signup", "/css/**", "/js/**",
                                "/actuator/health", "/error").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error"))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            if (apiRequest.matches(request)) {
                                authenticationEntryPoint.commence(request, response, exception);
                            } else {
                                webAuthenticationEntryPoint.commence(request, response, exception);
                            }
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            if (apiRequest.matches(request)) {
                                accessDeniedHandler.handle(request, response, exception);
                            } else {
                                webAccessDeniedHandler.handle(request, response, exception);
                            }
                        }));
        return http.build();
    }
}
