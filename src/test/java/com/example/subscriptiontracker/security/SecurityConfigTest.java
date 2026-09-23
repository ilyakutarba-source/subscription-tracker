package com.example.subscriptiontracker.security;

import com.example.subscriptiontracker.domain.User;
import com.example.subscriptiontracker.domain.UserRole;
import com.example.subscriptiontracker.service.RegistrationService;
import com.example.subscriptiontracker.web.AccountController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(AccountController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, SecurityConfigTest.TestUsers.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    @Test
    void loginPageRendersWithCsrfProtectedForm() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void unauthenticatedWebPageRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/subscriptions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void unauthenticatedApiReturnsUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signupPostRequiresCsrf() throws Exception {
        mockMvc.perform(post("/signup"))
                .andExpect(status().isForbidden());
    }

    @Test
    void regularUserCannotOpenAdminArea() throws Exception {
        mockMvc.perform(get("/admin").with(user("person@example.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void correctCredentialsAuthenticate() throws Exception {
        MvcResult result = mockMvc.perform(post("/login").with(csrf())
                        .param("email", "person@example.com")
                        .param("password", "correct-horse"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        SecurityContext context = (SecurityContext) result.getRequest().getSession()
                .getAttribute("SPRING_SECURITY_CONTEXT");
        AccountPrincipal principal = (AccountPrincipal) context.getAuthentication().getPrincipal();
        assertThat(principal.getPassword()).isNull();
    }

    @Test
    void logoutEndsAuthenticatedSession() throws Exception {
        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("email", "person@example.com")
                        .param("password", "correct-horse"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void wrongPasswordIsRejectedGenerically() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("email", "person@example.com")
                        .param("password", "wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void unknownEmailIsRejectedGenerically() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("email", "unknown@example.com")
                        .param("password", "correct-horse"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @TestConfiguration
    static class TestUsers {

        @Bean
        UserDetailsService userDetailsService(org.springframework.security.crypto.password.PasswordEncoder encoder) {
            User user = new User(UUID.randomUUID(), "person@example.com",
                    encoder.encode("correct-horse"), "Person", UserRole.USER, true);
            return email -> {
                if (!user.getEmail().equals(email)) {
                    throw new UsernameNotFoundException("Invalid email or password.");
                }
                return AccountPrincipal.from(user);
            };
        }
    }
}
