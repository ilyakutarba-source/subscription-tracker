package com.example.subscriptiontracker.web;

import com.example.subscriptiontracker.api.SubscriptionMapper;
import com.example.subscriptiontracker.api.dto.DashboardResponse;
import com.example.subscriptiontracker.domain.BillingPeriod;
import com.example.subscriptiontracker.domain.Subscription;
import com.example.subscriptiontracker.domain.SubscriptionCategory;
import com.example.subscriptiontracker.domain.SubscriptionStatus;
import com.example.subscriptiontracker.domain.User;
import com.example.subscriptiontracker.domain.UserRole;
import com.example.subscriptiontracker.exception.SubscriptionNotFoundException;
import com.example.subscriptiontracker.service.DashboardService;
import com.example.subscriptiontracker.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class WebControllerTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private SubscriptionService subscriptionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new WebController(dashboardService, subscriptionService, new SubscriptionMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new WebExceptionHandler())
                .setViewResolvers(viewResolver())
                .build();
    }

    @Test
    void dashboardRenders() throws Exception {
        var upcoming = new SubscriptionMapper().toResponse(subscription(UUID.randomUUID()));
        when(dashboardService.getDashboard(30)).thenReturn(
                new DashboardResponse(1, Map.of("USD", new BigDecimal("20.00")),
                        Map.of("USD", new BigDecimal("240.00")), List.of(upcoming)));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("$20.00")));
    }

    @Test
    void subscriptionListRenders() throws Exception {
        when(subscriptionService.findAll()).thenReturn(List.of(subscription(UUID.randomUUID())));

        mockMvc.perform(get("/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(view().name("subscriptions/list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ChatGPT")));
    }

    @Test
    void addFormRenders() throws Exception {
        mockMvc.perform(get("/subscriptions/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("subscriptions/form"))
                .andExpect(model().attribute("editing", false))
                .andExpect(model().attributeExists("subscriptionForm"));
    }

    @Test
    void validCreateRedirectsWithFeedback() throws Exception {
        mockMvc.perform(validForm(post("/subscriptions")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscriptions"))
                .andExpect(flash().attribute("successMessage", "Subscription added successfully."));

        verify(subscriptionService).create(any());
    }

    @Test
    void invalidCreatePreservesFormAndShowsErrors() throws Exception {
        mockMvc.perform(post("/subscriptions")
                        .param("name", "")
                        .param("price", "0")
                        .param("currency", "US"))
                .andExpect(status().isOk())
                .andExpect(view().name("subscriptions/form"))
                .andExpect(model().attributeHasFieldErrors("subscriptionForm", "name", "price", "currency"));
    }

    @Test
    void editFormIsPrepopulated() throws Exception {
        UUID id = UUID.randomUUID();
        when(subscriptionService.findById(id)).thenReturn(subscription(id));

        mockMvc.perform(get("/subscriptions/{id}/edit", id))
                .andExpect(status().isOk())
                .andExpect(view().name("subscriptions/form"))
                .andExpect(model().attribute("editing", true))
                .andExpect(model().attributeExists("subscriptionForm"));
    }

    @Test
    void missingEditRendersHumanNotFoundPage() throws Exception {
        UUID id = UUID.randomUUID();
        when(subscriptionService.findById(id)).thenThrow(new SubscriptionNotFoundException(id));

        mockMvc.perform(get("/subscriptions/{id}/edit", id))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Subscription not found")));
    }

    @Test
    void validEditRedirectsWithFeedback() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(validForm(post("/subscriptions/{id}", id)).param("status", "PAUSED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscriptions"))
                .andExpect(flash().attribute("successMessage", "Subscription updated successfully."));

        verify(subscriptionService).update(eq(id), any());
    }

    @Test
    void cancelRedirectsWithFeedback() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/subscriptions/{id}/cancel", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscriptions"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(subscriptionService).cancel(id);
    }

    @Test
    void deleteRedirectsWithFeedback() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/subscriptions/{id}/delete", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscriptions"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(subscriptionService).delete(id);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validForm(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) {
        return request
                .param("name", "ChatGPT")
                .param("description", "AI assistant")
                .param("price", "20.00")
                .param("currency", "USD")
                .param("billingPeriod", "MONTHLY")
                .param("startDate", "2026-01-01")
                .param("nextPaymentDate", "2026-02-01")
                .param("category", "SOFTWARE");
    }

    private Subscription subscription(UUID id) {
        User user = new User(UUID.randomUUID(), "user@example.com", "hash", "User", UserRole.USER, true);
        return new Subscription(id, user, "ChatGPT", "AI assistant", new BigDecimal("20.00"), "USD",
                BillingPeriod.MONTHLY, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1),
                SubscriptionCategory.SOFTWARE, SubscriptionStatus.ACTIVE);
    }

    private ThymeleafViewResolver viewResolver() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine);
        viewResolver.setCharacterEncoding("UTF-8");
        return viewResolver;
    }
}
