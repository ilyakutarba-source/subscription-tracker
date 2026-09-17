package com.example.subscriptiontracker.web;

import com.example.subscriptiontracker.api.dto.SubscriptionRequest;
import com.example.subscriptiontracker.domain.BillingPeriod;
import com.example.subscriptiontracker.domain.Subscription;
import com.example.subscriptiontracker.domain.SubscriptionCategory;
import com.example.subscriptiontracker.domain.SubscriptionStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SubscriptionForm {

    @NotBlank(message = "Enter a subscription name.")
    @Size(max = 120, message = "Name must be 120 characters or fewer.")
    private String name;

    @Size(max = 1000, message = "Description must be 1000 characters or fewer.")
    private String description;

    @NotNull(message = "Enter a price.")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0.")
    private BigDecimal price;

    @NotBlank(message = "Enter a currency code.")
    @Pattern(regexp = "(?i)[A-Z]{3}", message = "Use a 3-letter ISO currency code.")
    private String currency;

    @NotNull(message = "Choose a billing period.")
    private BillingPeriod billingPeriod;

    @NotNull(message = "Choose a start date.")
    private LocalDate startDate;

    @NotNull(message = "Choose the next payment date.")
    private LocalDate nextPaymentDate;

    @NotNull(message = "Choose a category.")
    private SubscriptionCategory category;

    private SubscriptionStatus status;

    public static SubscriptionForm empty() {
        SubscriptionForm form = new SubscriptionForm();
        form.currency = "USD";
        form.billingPeriod = BillingPeriod.MONTHLY;
        form.category = SubscriptionCategory.OTHER;
        form.startDate = LocalDate.now();
        form.nextPaymentDate = LocalDate.now();
        return form;
    }

    public static SubscriptionForm from(Subscription subscription) {
        SubscriptionForm form = new SubscriptionForm();
        form.name = subscription.getName();
        form.description = subscription.getDescription();
        form.price = subscription.getPrice();
        form.currency = subscription.getCurrency();
        form.billingPeriod = subscription.getBillingPeriod();
        form.startDate = subscription.getStartDate();
        form.nextPaymentDate = subscription.getNextPaymentDate();
        form.category = subscription.getCategory();
        form.status = subscription.getStatus();
        return form;
    }

    public SubscriptionRequest toRequest() {
        return new SubscriptionRequest(name, description, price, currency, billingPeriod,
                startDate, nextPaymentDate, category, status);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BillingPeriod getBillingPeriod() { return billingPeriod; }
    public void setBillingPeriod(BillingPeriod billingPeriod) { this.billingPeriod = billingPeriod; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getNextPaymentDate() { return nextPaymentDate; }
    public void setNextPaymentDate(LocalDate nextPaymentDate) { this.nextPaymentDate = nextPaymentDate; }
    public SubscriptionCategory getCategory() { return category; }
    public void setCategory(SubscriptionCategory category) { this.category = category; }
    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }
}
