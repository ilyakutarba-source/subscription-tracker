package com.example.subscriptiontracker.web;

import com.example.subscriptiontracker.domain.UserRole;
import com.example.subscriptiontracker.service.AdminOverview;
import com.example.subscriptiontracker.service.AdminService;
import com.example.subscriptiontracker.service.AdminUserSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminController(adminService))
                .setViewResolvers(WebControllerTestSupport.viewResolver())
                .build();
    }

    @Test
    void adminPageRendersOverviewAndUsers() throws Exception {
        when(adminService.getOverview()).thenReturn(new AdminOverview(2, 2, 1, 3));
        when(adminService.findUsers()).thenReturn(List.of(new AdminUserSummary(
                UUID.randomUUID(), "admin@example.com", "Admin", UserRole.ADMIN,
                true, 3, Instant.parse("2026-01-01T00:00:00Z"), true)));

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/index"))
                .andExpect(model().attributeExists("overview", "users", "roles"))
                .andExpect(content().string(containsString("User management")))
                .andExpect(content().string(containsString("admin@example.com")));
    }

    @Test
    void roleChangeRedirectsWithFeedback() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/admin/users/{id}/role", userId).param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("successMessage", "User role updated."));

        verify(adminService).changeRole(userId, UserRole.ADMIN);
    }

    @Test
    void blockedSelfActionReturnsReadableFeedback() throws Exception {
        UUID userId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new IllegalStateException("You cannot block your own account."))
                .when(adminService).changeEnabled(userId, false);

        mockMvc.perform(post("/admin/users/{id}/status", userId).param("enabled", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("errorMessage", "You cannot block your own account."));
    }
}
