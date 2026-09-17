package com.example.subscriptiontracker.api;

import com.example.subscriptiontracker.domain.BillingPeriod;
import com.example.subscriptiontracker.domain.Subscription;
import com.example.subscriptiontracker.domain.SubscriptionCategory;
import com.example.subscriptiontracker.domain.SubscriptionStatus;
import com.example.subscriptiontracker.exception.GlobalExceptionHandler;
import com.example.subscriptiontracker.exception.SubscriptionNotFoundException;
import com.example.subscriptiontracker.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    private SubscriptionService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new SubscriptionController(service, new SubscriptionMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void postValidReturnsCreated() throws Exception {
        when(service.create(any())).thenReturn(subscription(SubscriptionStatus.ACTIVE));

        mockMvc.perform(post("/api/v1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("ChatGPT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void postInvalidReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    @Test
    void getExistingReturnsSubscription() throws Exception {
        UUID id = UUID.randomUUID();
        Subscription existing = new Subscription(id, "ChatGPT", "AI", new BigDecimal("20.00"), "USD",
                BillingPeriod.MONTHLY, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1),
                SubscriptionCategory.SOFTWARE, SubscriptionStatus.ACTIVE);
        when(service.findById(id)).thenReturn(existing);

        mockMvc.perform(get("/api/v1/subscriptions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("ChatGPT"));
    }

    @Test
    void getMissingReturnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.findById(id)).thenThrow(new SubscriptionNotFoundException(id));

        mockMvc.perform(get("/api/v1/subscriptions/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void cancelActiveReturnsCancelled() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.cancel(id)).thenReturn(subscription(SubscriptionStatus.CANCELLED));

        mockMvc.perform(patch("/api/v1/subscriptions/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    private Subscription subscription(SubscriptionStatus status) {
        return new Subscription(UUID.randomUUID(), "ChatGPT", "AI", new BigDecimal("20.00"), "USD",
                BillingPeriod.MONTHLY, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1),
                SubscriptionCategory.SOFTWARE, status);
    }

    private String validJson() {
        return """
                {
                  "name": "ChatGPT",
                  "description": "AI",
                  "price": 20.00,
                  "currency": "USD",
                  "billingPeriod": "MONTHLY",
                  "startDate": "2026-01-01",
                  "nextPaymentDate": "2026-02-01",
                  "category": "SOFTWARE"
                }
                """;
    }
}
