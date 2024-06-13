package com.example.demo.controller;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrometheusController {

    private final PrometheusMeterRegistry prometheusMeterRegistry;

    public PrometheusController(PrometheusMeterRegistry prometheusMeterRegistry) {
        this.prometheusMeterRegistry = prometheusMeterRegistry;
    }

    @GetMapping("/metrics")
    public String scrape() {
        return prometheusMeterRegistry.scrape();
    }
}
