package com.dpx.repository;

import com.dpx.model.Delivery;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * In-memory repository for Delivery records.
 * Uses a LinkedHashMap to maintain insertion order.
 */
@Repository
public class DeliveryRepository {

    private final Map<String, Delivery> store = new LinkedHashMap<>();

    public DeliveryRepository() {
        // Pre-load sample data for demonstration
        save(new Delivery("DPX001", "Anna Smith",   "John Doe",     "123 Main St, Riga",       2.5));
        save(new Delivery("DPX002", "Bob Johnson",  "Maria Garcia", "456 Oak Ave, Jurmala",    0.8));
        save(new Delivery("DPX003", "Carol White",  "Peter Brown",  "789 Pine Rd, Liepaja",    5.2));
        save(new Delivery("DPX004", "David Green",  "Lisa Turner",  "321 Elm St, Daugavpils",  1.1));
        save(new Delivery("DPX005", "Eve Black",    "Tom Wilson",   "654 Birch Ln, Ventspils", 3.7));

        // Set varied statuses for a realistic demo
        findById("DPX002").ifPresent(d -> d.setStatus("In Transit"));
        findById("DPX003").ifPresent(d -> d.setStatus("Delivered"));
        findById("DPX004").ifPresent(d -> d.setStatus("In Transit"));
    }

    /** Return all deliveries as a list */
    public List<Delivery> findAll() {
        return new ArrayList<>(store.values());
    }

    /** Find a single delivery by ID */
    public Optional<Delivery> findById(String id) {
        return Optional.ofNullable(store.get(id.toUpperCase()));
    }

    /** Check if an ID already exists */
    public boolean existsById(String id) {
        return store.containsKey(id.toUpperCase());
    }

    /** Save (create or update) a delivery */
    public Delivery save(Delivery delivery) {
        delivery.setId(delivery.getId().toUpperCase());
        store.put(delivery.getId(), delivery);
        return delivery;
    }

    /** Delete a delivery by ID */
    public boolean deleteById(String id) {
        return store.remove(id.toUpperCase()) != null;
    }

    /** Count deliveries by status */
    public Map<String, Long> countByStatus() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("Pending",    store.values().stream().filter(d -> "Pending".equals(d.getStatus())).count());
        counts.put("In Transit", store.values().stream().filter(d -> "In Transit".equals(d.getStatus())).count());
        counts.put("Delivered",  store.values().stream().filter(d -> "Delivered".equals(d.getStatus())).count());
        counts.put("Cancelled",  store.values().stream().filter(d -> "Cancelled".equals(d.getStatus())).count());
        return counts;
    }
}
