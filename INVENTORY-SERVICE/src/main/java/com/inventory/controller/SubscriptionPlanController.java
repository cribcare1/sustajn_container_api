package com.inventory.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.inventory.entity.SubscriptionPlan;
import com.inventory.service.SubscriptionPlanService;

@RestController
@RequestMapping("/inventory/subscription-plans")
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanService service;

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody SubscriptionPlan plan) {
        Map<String, Object> resp = service.createSubscriptionPlan(plan);
        String status = (String) resp.getOrDefault("status", "error");
        HttpStatus code = "success".equals(status) ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(resp, code);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Integer id) {
        Map<String, Object> resp = service.getSubscriptionPlanById(id);
        String status = (String) resp.getOrDefault("status", "error");
        HttpStatus code = "success".equals(status) ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        return new ResponseEntity<>(resp, code);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Integer id, @RequestBody SubscriptionPlan plan) {
        Map<String, Object> resp = service.updateSubscriptionPlan(id, plan);
        String status = (String) resp.getOrDefault("status", "error");
        HttpStatus code = "success".equals(status) ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(resp, code);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Integer id) {
        Map<String, Object> resp = service.deleteSubscriptionPlan(id);
        String status = (String) resp.getOrDefault("status", "error");
        HttpStatus code = "success".equals(status) ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        return new ResponseEntity<>(resp, code);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listAll() {
        Map<String, Object> resp = service.listAllPlans();
        String status = (String) resp.getOrDefault("status", "error");
        HttpStatus code = "success".equals(status) ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
        return new ResponseEntity<>(resp, code);
    }

    @GetMapping("/summaries")
    public ResponseEntity<Map<String, Object>> summaries(@RequestParam(required = false) String status) {
        SubscriptionPlan.PlanStatus ps = null;
        if (status != null && !status.isBlank()) {
            try {
                ps = SubscriptionPlan.PlanStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                Map<String, Object> bad = Map.of("message", "Invalid status value", "status", "bad_request", "data", null);
                return new ResponseEntity<>(bad, HttpStatus.BAD_REQUEST);
            }
        }
        Map<String, Object> resp = service.getPlanSummaries(ps);
        String st = (String) resp.getOrDefault("status", "error");
        HttpStatus code = "success".equals(st) ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
        return new ResponseEntity<>(resp, code);
    }
}
