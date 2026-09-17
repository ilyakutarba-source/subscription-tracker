package com.example.subscriptiontracker.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class WebViewFormatter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH);

    public String money(BigDecimal amount, String currency) {
        String value = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        return switch (currency) {
            case "USD" -> "$" + value;
            case "EUR" -> "€" + value;
            default -> value + " " + currency;
        };
    }

    public String date(LocalDate date) {
        return date.format(DATE);
    }

    public String label(Enum<?> value) {
        String text = value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
