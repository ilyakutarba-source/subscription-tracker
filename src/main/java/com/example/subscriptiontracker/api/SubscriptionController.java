package com.example.subscriptiontracker.api;

import com.example.subscriptiontracker.api.dto.SubscriptionRequest;
import com.example.subscriptiontracker.api.dto.SubscriptionResponse;
import com.example.subscriptiontracker.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionService service;
    private final SubscriptionMapper mapper;

    public SubscriptionController(SubscriptionService service, SubscriptionMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<SubscriptionResponse> findAll() {
        return service.findAll().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public SubscriptionResponse findById(@PathVariable UUID id) {
        return mapper.toResponse(service.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse create(@Valid @RequestBody SubscriptionRequest request) {
        return mapper.toResponse(service.create(request));
    }

    @PutMapping("/{id}")
    public SubscriptionResponse update(@PathVariable UUID id,
                                       @Valid @RequestBody SubscriptionRequest request) {
        return mapper.toResponse(service.update(id, request));
    }

    @PatchMapping("/{id}/cancel")
    public SubscriptionResponse cancel(@PathVariable UUID id) {
        return mapper.toResponse(service.cancel(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}

