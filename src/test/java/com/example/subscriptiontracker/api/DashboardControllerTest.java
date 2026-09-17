package com.example.subscriptiontracker.api;

import com.example.subscriptiontracker.api.dto.DashboardResponse;
import com.example.subscriptiontracker.exception.GlobalExceptionHandler;
import com.example.subscriptiontracker.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DashboardController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void dashboardReturnsAggregates() throws Exception {
        when(service.getDashboard(30)).thenReturn(new DashboardResponse(
                2, Map.of("USD", new BigDecimal("42.50")),
                Map.of("USD", new BigDecimal("510.00")), List.of()));

        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeSubscriptions").value(2))
                .andExpect(jsonPath("$.monthlyCost.USD").value(42.50))
                .andExpect(jsonPath("$.yearlyCost.USD").value(510.00));
    }
}

