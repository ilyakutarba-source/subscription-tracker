package com.example.subscriptiontracker.web;

import com.example.subscriptiontracker.domain.UserRole;
import com.example.subscriptiontracker.service.AdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("overview", adminService.getOverview());
        model.addAttribute("users", adminService.findUsers());
        model.addAttribute("roles", UserRole.values());
        return "admin/index";
    }

    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable UUID id, @RequestParam UserRole role,
                             RedirectAttributes redirectAttributes) {
        try {
            adminService.changeRole(id, role);
            redirectAttributes.addFlashAttribute("successMessage", "User role updated.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/users/{id}/status")
    public String changeStatus(@PathVariable UUID id, @RequestParam boolean enabled,
                               RedirectAttributes redirectAttributes) {
        try {
            adminService.changeEnabled(id, enabled);
            redirectAttributes.addFlashAttribute("successMessage",
                    enabled ? "User unblocked." : "User blocked.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin";
    }
}
