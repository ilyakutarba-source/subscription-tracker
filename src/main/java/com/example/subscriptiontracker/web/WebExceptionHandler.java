package com.example.subscriptiontracker.web;

import com.example.subscriptiontracker.exception.SubscriptionNotFoundException;
import com.example.subscriptiontracker.exception.CurrentUserNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(assignableTypes = {WebController.class, AdminController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebExceptionHandler.class);

    @ExceptionHandler(SubscriptionNotFoundException.class)
    ModelAndView notFound(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return error("Subscription not found", "The subscription may have been removed or the link is no longer valid.");
    }

    @ExceptionHandler(CurrentUserNotFoundException.class)
    ModelAndView currentUserMissing(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return error("Please sign in again", "Your account session is no longer valid.");
    }

    @ExceptionHandler(Exception.class)
    ModelAndView unexpected(Exception exception, HttpServletResponse response) {
        LOGGER.error("Unexpected web interface error", exception);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return error("Something went wrong", "We couldn't complete that request. Please try again.");
    }

    private ModelAndView error(String title, String message) {
        ModelAndView view = new ModelAndView("error");
        view.addObject("errorTitle", title);
        view.addObject("errorMessage", message);
        return view;
    }
}
