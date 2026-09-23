package com.example.subscriptiontracker.web;

import com.example.subscriptiontracker.exception.DuplicateEmailException;
import com.example.subscriptiontracker.service.RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private RegistrationService registrationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AccountController(registrationService)).build();
    }

    @Test
    void signupPageRenders() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/signup"))
                .andExpect(model().attributeExists("signupForm"));
    }

    @Test
    void validSignupRegistersAndRedirectsToLogin() throws Exception {
        mockMvc.perform(validSignup())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        verify(registrationService).register("person@example.com", "correct-horse", "Person");
    }

    @Test
    void invalidEmailAndShortPasswordAreRejected() throws Exception {
        mockMvc.perform(post("/signup")
                        .param("displayName", "Person")
                        .param("email", "not-an-email")
                        .param("password", "short")
                        .param("confirmPassword", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/signup"))
                .andExpect(model().attributeHasFieldErrors("signupForm", "email", "password"));
    }

    @Test
    void mismatchedPasswordsAreRejected() throws Exception {
        mockMvc.perform(validSignup().param("confirmPassword", "different-password"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("signupForm", "confirmPassword"));
    }

    @Test
    void duplicateEmailIsShownAsFieldError() throws Exception {
        doThrow(new DuplicateEmailException()).when(registrationService)
                .register(anyString(), anyString(), anyString());

        mockMvc.perform(validSignup())
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("signupForm", "email"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validSignup() {
        return post("/signup")
                .param("displayName", "Person")
                .param("email", "person@example.com")
                .param("password", "correct-horse")
                .param("confirmPassword", "correct-horse");
    }
}
