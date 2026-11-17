package dpx;

/** The five entities of the DPX data model. */
public class Entities {
    public record Customer(long id, String name, String phone, String email) {}

    public record Courier(long id, String name, String vehicle, boolean active) {}

    public record Parcel(long id, long customerId, String description, double weightKg) {}

    public record Delivery(long id, long parcelId, long courierId, String address, String status) {}

    public record StatusHistory(long id, long deliveryId, String status, String changedAt) {}
}
