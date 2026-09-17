package com.example.subscriptiontracker.api;

import com.example.subscriptiontracker.api.dto.DashboardResponse;
import com.example.subscriptiontracker.service.DashboardService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public DashboardResponse dashboard(
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        return service.getDashboard(days);
    }
}

