package com.example.subscriptiontracker.web;

import com.example.subscriptiontracker.api.SubscriptionMapper;
import com.example.subscriptiontracker.domain.BillingPeriod;
import com.example.subscriptiontracker.domain.SubscriptionCategory;
import com.example.subscriptiontracker.domain.SubscriptionStatus;
import com.example.subscriptiontracker.service.DashboardService;
import com.example.subscriptiontracker.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping
public class WebController {

    private static final int UPCOMING_DAYS = 30;

    private final DashboardService dashboardService;
    private final SubscriptionService subscriptionService;
    private final SubscriptionMapper mapper;

    @ModelAttribute("ui")
    public WebViewFormatter viewFormatter() {
        return new WebViewFormatter();
    }

    public WebController(DashboardService dashboardService, SubscriptionService subscriptionService,
                         SubscriptionMapper mapper) {
        this.dashboardService = dashboardService;
        this.subscriptionService = subscriptionService;
        this.mapper = mapper;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("dashboard", dashboardService.getDashboard(UPCOMING_DAYS));
        model.addAttribute("upcomingDays", UPCOMING_DAYS);
        return "dashboard";
    }

    @GetMapping("/subscriptions")
    public String subscriptions(Model model) {
        model.addAttribute("subscriptions", subscriptionService.findAll().stream()
                .map(mapper::toResponse)
                .toList());
        return "subscriptions/list";
    }

    @GetMapping("/subscriptions/new")
    public String newSubscription(Model model) {
        model.addAttribute("subscriptionForm", SubscriptionForm.empty());
        return form(model, false, null);
    }

    @PostMapping("/subscriptions")
    public String create(@Valid SubscriptionForm subscriptionForm, BindingResult bindingResult,
                         Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return form(model, false, null);
        }
        try {
            subscriptionService.create(subscriptionForm.toRequest());
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("currency", "currency.invalid", "Enter a valid ISO currency code.");
            return form(model, false, null);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Subscription added successfully.");
        return "redirect:/subscriptions";
    }

    @GetMapping("/subscriptions/{id}/edit")
    public String editSubscription(@PathVariable UUID id, Model model) {
        var subscription = subscriptionService.findById(id);
        model.addAttribute("subscriptionForm", SubscriptionForm.from(subscription));
        return form(model, true, id);
    }

    @PostMapping("/subscriptions/{id}")
    public String update(@PathVariable UUID id, @Valid SubscriptionForm subscriptionForm,
                         BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (subscriptionForm.getStatus() == null) {
            bindingResult.rejectValue("status", "status.required", "Choose a status.");
        }
        if (bindingResult.hasErrors()) {
            return form(model, true, id);
        }
        try {
            subscriptionService.update(id, subscriptionForm.toRequest());
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("currency", "currency.invalid", "Enter a valid ISO currency code.");
            return form(model, true, id);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Subscription updated successfully.");
        return "redirect:/subscriptions";
    }

    @PostMapping("/subscriptions/{id}/cancel")
    public String cancel(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        subscriptionService.cancel(id);
        redirectAttributes.addFlashAttribute("successMessage", "Subscription cancelled. It remains in your history.");
        return "redirect:/subscriptions";
    }

    @PostMapping("/subscriptions/{id}/delete")
    public String delete(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        subscriptionService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Subscription permanently deleted.");
        return "redirect:/subscriptions";
    }

    private String form(Model model, boolean editing, UUID id) {
        model.addAttribute("editing", editing);
        model.addAttribute("subscriptionId", id);
        model.addAttribute("billingPeriods", BillingPeriod.values());
        model.addAttribute("categories", SubscriptionCategory.values());
        model.addAttribute("statuses", SubscriptionStatus.values());
        return "subscriptions/form";
    }
}
