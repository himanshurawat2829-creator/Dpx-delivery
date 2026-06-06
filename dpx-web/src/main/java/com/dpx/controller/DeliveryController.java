package com.dpx.controller;

import com.dpx.model.Delivery;
import com.dpx.repository.DeliveryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST Controller for the DPX Courier Delivery Management System.
 *
 * Endpoints:
 *   GET    /api/deliveries          → list all
 *   GET    /api/deliveries/{id}     → get one by ID
 *   POST   /api/deliveries          → create new
 *   PUT    /api/deliveries/{id}     → update existing
 *   DELETE /api/deliveries/{id}     → delete
 *   GET    /api/deliveries/stats    → statistics
 */
@RestController
@RequestMapping("/api/deliveries")
@CrossOrigin(origins = "*")
public class DeliveryController {

    private final DeliveryRepository repository;

    public DeliveryController(DeliveryRepository repository) {
        this.repository = repository;
    }

    // ── GET ALL ───────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<Delivery>> getAll(
            @RequestParam(required = false) String search) {

        List<Delivery> all = repository.findAll();

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            all = all.stream()
                .filter(d -> d.getId().toLowerCase().contains(q)
                          || d.getRecipientName().toLowerCase().contains(q)
                          || d.getSenderName().toLowerCase().contains(q)
                          || d.getAddress().toLowerCase().contains(q))
                .toList();
        }

        return ResponseEntity.ok(all);
    }

    // ── GET ONE BY ID ─────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return repository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Delivery delivery) {
        if (delivery.getId() == null || delivery.getId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Delivery ID is required."));
        }
        if (repository.existsById(delivery.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Delivery ID already exists: " + delivery.getId()));
        }
        if (delivery.getWeight() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Weight must be greater than 0."));
        }
        if (delivery.getStatus() == null || delivery.getStatus().isBlank()) {
            delivery.setStatus("Pending");
        }
        Delivery saved = repository.save(delivery);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id,
                                     @RequestBody Delivery updated) {
        return repository.findById(id).map(existing -> {
            if (updated.getSenderName()    != null) existing.setSenderName(updated.getSenderName());
            if (updated.getRecipientName() != null) existing.setRecipientName(updated.getRecipientName());
            if (updated.getAddress()       != null) existing.setAddress(updated.getAddress());
            if (updated.getWeight()        >  0   ) existing.setWeight(updated.getWeight());
            if (updated.getStatus()        != null) existing.setStatus(updated.getStatus());
            repository.save(existing);
            return ResponseEntity.ok(existing);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        if (repository.deleteById(id)) {
            return ResponseEntity.ok(Map.of("message", "Delivery " + id + " deleted successfully."));
        }
        return ResponseEntity.notFound().build();
    }

    // ── STATISTICS ────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        List<Delivery> all = repository.findAll();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", all.size());
        result.putAll(repository.countByStatus());
        return ResponseEntity.ok(result);
    }
}
