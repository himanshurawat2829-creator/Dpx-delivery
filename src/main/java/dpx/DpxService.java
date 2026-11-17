package dpx;

import dpx.Entities.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Business logic of the DPX system. The handle() method receives an HTTP method,
 * a path like "/api/customers/3" and a JSON body, and returns a Response.
 */
public class DpxService {

    public record Response(int status, String body) {}

    public static final List<String> STATUSES = List.of("Pending", "In Transit", "Delivered", "Cancelled");

    // allowed status changes
    private static final Map<String, List<String>> NEXT = Map.of(
            "Pending", List.of("In Transit", "Cancelled"),
            "In Transit", List.of("Delivered", "Cancelled"),
            "Delivered", List.of(),
            "Cancelled", List.of());

    final Repo<Customer> customers = new Repo<>(Customer.class);
    final Repo<Courier> couriers = new Repo<>(Courier.class);
    final Repo<Parcel> parcels = new Repo<>(Parcel.class);
    final Repo<Delivery> deliveries = new Repo<>(Delivery.class);
    final Repo<StatusHistory> history = new Repo<>(StatusHistory.class);

    // ---------------- routing ----------------

    public Response handle(String method, String path, String body) {
        try {
            String[] parts = path.replaceAll("^/+|/+$", "").split("/");
            if (parts.length < 2 || !parts[0].equals("api")) throw new ApiException(404, "Unknown path");
            String name = parts[1];
            Long id = parts.length > 2 ? parseId(parts[2]) : null;
            if (parts.length > 3) throw new ApiException(404, "Unknown path");
            Map<String, Object> data = Json.parse(body);

            if (name.equals("stats") && method.equals("GET")) return ok(stats());
            switch (name) {
                case "customers": return crud(method, id, data, customers, this::checkCustomer, this::beforeDeleteCustomer);
                case "couriers": return crud(method, id, data, couriers, this::checkCourier, this::beforeDeleteCourier);
                case "packages": return crud(method, id, data, parcels, this::checkParcel, this::beforeDeleteParcel);
                case "deliveries": return deliveryRoute(method, id, data);
                case "history": return historyRoute(method, id);
                default: throw new ApiException(404, "Unknown path");
            }
        } catch (ApiException e) {
            return new Response(e.status, Json.toJson(Map.of("error", e.getMessage())));
        } catch (IllegalArgumentException e) {
            return new Response(400, Json.toJson(Map.of("error", "Invalid JSON: " + e.getMessage())));
        }
    }

    private <T extends Record> Response crud(String method, Long id, Map<String, Object> data, Repo<T> repo,
                                             java.util.function.BiConsumer<Map<String, Object>, Boolean> check,
                                             java.util.function.LongConsumer beforeDelete) {
        switch (method) {
            case "GET":
                if (id == null) return ok(repo.findAll());
                return ok(repo.findById(id).orElseThrow(() -> new ApiException(404, "Not found")));
            case "POST":
                if (id != null) throw new ApiException(405, "POST does not use an id");
                check.accept(data, true);
                return new Response(201, Json.toJson(repo.create(data)));
            case "PUT":
                needId(id);
                repo.findById(id).orElseThrow(() -> new ApiException(404, "Not found"));
                check.accept(data, false);
                return ok(repo.update(id, data));
            case "DELETE":
                needId(id);
                repo.findById(id).orElseThrow(() -> new ApiException(404, "Not found"));
                beforeDelete.accept(id);
                repo.delete(id);
                return ok(Map.of("deleted", id));
            default:
                throw new ApiException(405, "Method not allowed");
        }
    }

    // ---------------- deliveries (special: status rules + history) ----------------

    private Response deliveryRoute(String method, Long id, Map<String, Object> data) {
        switch (method) {
            case "GET":
                if (id == null) return ok(deliveries.findAll());
                return ok(deliveries.findById(id).orElseThrow(() -> new ApiException(404, "Not found")));
            case "POST": {
                checkDelivery(data, true);
                data.put("status", "Pending");
                Delivery d = deliveries.create(data);
                addHistory(d.id(), d.status());
                return new Response(201, Json.toJson(d));
            }
            case "PUT": {
                needId(id);
                Delivery old = deliveries.findById(id).orElseThrow(() -> new ApiException(404, "Not found"));
                checkDelivery(data, false);
                if (data.containsKey("status") && !data.get("status").equals(old.status())
                        && !NEXT.get(old.status()).contains(data.get("status")))
                    throw new ApiException(409, "Cannot change status from " + old.status() + " to " + data.get("status"));
                Delivery d = deliveries.update(id, data);
                if (!d.status().equals(old.status())) addHistory(id, d.status());
                return ok(d);
            }
            case "DELETE":
                needId(id);
                deliveries.findById(id).orElseThrow(() -> new ApiException(404, "Not found"));
                for (StatusHistory h : history.findAll())
                    if (h.deliveryId() == id) history.delete(h.id());
                deliveries.delete(id);
                return ok(Map.of("deleted", id));
            default:
                throw new ApiException(405, "Method not allowed");
        }
    }

    private Response historyRoute(String method, Long id) {
        if (!method.equals("GET")) throw new ApiException(405, "History is read-only");
        if (id != null) return ok(history.findById(id).orElseThrow(() -> new ApiException(404, "Not found")));
        return ok(history.findAll());
    }

    private void addHistory(long deliveryId, String status) {
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("deliveryId", (double) deliveryId);
        h.put("status", status);
        h.put("changedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        history.create(h);
    }

    // ---------------- validation ----------------

    private void checkCustomer(Map<String, Object> d, Boolean isNew) {
        if (isNew || d.containsKey("name")) text(d, "name");
        if (isNew || d.containsKey("phone")) text(d, "phone");
    }

    private void checkCourier(Map<String, Object> d, Boolean isNew) {
        if (isNew || d.containsKey("name")) text(d, "name");
        if (isNew || d.containsKey("vehicle")) text(d, "vehicle");
        if (isNew && !d.containsKey("active")) d.put("active", Boolean.TRUE);
    }

    private void checkParcel(Map<String, Object> d, Boolean isNew) {
        if (isNew || d.containsKey("description")) text(d, "description");
        if (isNew || d.containsKey("customerId"))
            if (customers.findById(num(d, "customerId").longValue()).isEmpty())
                throw new ApiException(400, "Customer does not exist");
        if (isNew || d.containsKey("weightKg"))
            if (num(d, "weightKg") <= 0) throw new ApiException(400, "weightKg must be more than 0");
    }

    private void checkDelivery(Map<String, Object> d, Boolean isNew) {
        if (isNew || d.containsKey("address")) text(d, "address");
        if (isNew || d.containsKey("parcelId"))
            if (parcels.findById(num(d, "parcelId").longValue()).isEmpty())
                throw new ApiException(400, "Parcel does not exist");
        if (isNew || d.containsKey("courierId")) {
            Optional<Courier> c = couriers.findById(num(d, "courierId").longValue());
            if (c.isEmpty()) throw new ApiException(400, "Courier does not exist");
            if (!c.get().active()) throw new ApiException(400, "Courier is not active");
        }
        if (!isNew && d.containsKey("status")) {
            String s = String.valueOf(d.get("status"));
            if (!STATUSES.contains(s)) throw new ApiException(400, "Unknown status: " + s);
        }
    }

    private void beforeDeleteCustomer(long id) {
        for (Parcel p : parcels.findAll())
            if (p.customerId() == id) throw new ApiException(409, "Customer still has parcels");
    }

    private void beforeDeleteCourier(long id) {
        for (Delivery d : deliveries.findAll())
            if (d.courierId() == id) throw new ApiException(409, "Courier still has deliveries");
    }

    private void beforeDeleteParcel(long id) {
        for (Delivery d : deliveries.findAll())
            if (d.parcelId() == id) throw new ApiException(409, "Parcel still has a delivery");
    }

    private static void text(Map<String, Object> d, String key) {
        Object v = d.get(key);
        if (!(v instanceof String) || ((String) v).isBlank()) throw new ApiException(400, key + " is required");
    }

    private static Double num(Map<String, Object> d, String key) {
        Object v = d.get(key);
        if (!(v instanceof Number)) throw new ApiException(400, key + " must be a number");
        return ((Number) v).doubleValue();
    }

    private static Long parseId(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Id must be a number");
        }
    }

    private static void needId(Long id) {
        if (id == null) throw new ApiException(400, "Id is required");
    }

    private Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("customers", customers.findAll().size());
        m.put("couriers", couriers.findAll().size());
        m.put("packages", parcels.findAll().size());
        m.put("deliveries", deliveries.findAll().size());
        for (String s : STATUSES) {
            long n = deliveries.findAll().stream().filter(d -> d.status().equals(s)).count();
            m.put(s, n);
        }
        return m;
    }

    private static Response ok(Object o) {
        return new Response(200, Json.toJson(o));
    }

    /** Fills the system with a few records so the screen is not empty. */
    public void loadSampleData() {
        handle("POST", "/api/customers", "{\"name\":\"Anna Berzina\",\"phone\":\"+37120000001\",\"email\":\"anna@example.com\"}");
        handle("POST", "/api/customers", "{\"name\":\"John Smith\",\"phone\":\"+37120000002\",\"email\":\"john@example.com\"}");
        handle("POST", "/api/couriers", "{\"name\":\"Janis Ozols\",\"vehicle\":\"Bike\"}");
        handle("POST", "/api/couriers", "{\"name\":\"Marta Liepa\",\"vehicle\":\"Van\"}");
        handle("POST", "/api/packages", "{\"customerId\":1,\"description\":\"Books\",\"weightKg\":2.5}");
        handle("POST", "/api/packages", "{\"customerId\":2,\"description\":\"Laptop\",\"weightKg\":3.1}");
        handle("POST", "/api/deliveries", "{\"parcelId\":1,\"courierId\":1,\"address\":\"Brivibas iela 10, Riga\"}");
        handle("POST", "/api/deliveries", "{\"parcelId\":2,\"courierId\":2,\"address\":\"Liela iela 5, Jelgava\"}");
        handle("PUT", "/api/deliveries/1", "{\"status\":\"In Transit\"}");
    }
}
