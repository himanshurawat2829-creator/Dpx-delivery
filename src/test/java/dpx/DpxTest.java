package dpx;

import static org.junit.Assert.*;

import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;

public class DpxTest {
    private DpxService s;

    @Before
    public void setUp() {
        s = new DpxService();
        s.handle("POST", "/api/customers", "{\"name\":\"Anna\",\"phone\":\"123\"}");
        s.handle("POST", "/api/couriers", "{\"name\":\"Janis\",\"vehicle\":\"Bike\"}");
        s.handle("POST", "/api/packages", "{\"customerId\":1,\"description\":\"Books\",\"weightKg\":2}");
    }

    private int code(String m, String p, String b) {
        return s.handle(m, p, b).status();
    }

    private int newDelivery() {
        return code("POST", "/api/deliveries", "{\"parcelId\":1,\"courierId\":1,\"address\":\"Riga\"}");
    }

    // ---- Customer CRUD ----
    @Test public void customerCreateAndRead() {
        assertEquals(201, code("POST", "/api/customers", "{\"name\":\"Bob\",\"phone\":\"555\"}"));
        DpxService.Response r = s.handle("GET", "/api/customers/2", "");
        assertEquals(200, r.status());
        assertTrue(r.body().contains("\"name\":\"Bob\""));
    }
    @Test public void customerNameRequired() {
        assertEquals(400, code("POST", "/api/customers", "{\"phone\":\"555\"}"));
    }
    @Test public void customerUpdate() {
        assertEquals(200, code("PUT", "/api/customers/1", "{\"phone\":\"999\"}"));
        assertTrue(s.handle("GET", "/api/customers/1", "").body().contains("\"phone\":\"999\""));
        assertTrue(s.handle("GET", "/api/customers/1", "").body().contains("\"name\":\"Anna\""));
    }
    @Test public void customerNotFound() {
        assertEquals(404, code("GET", "/api/customers/99", ""));
    }
    @Test public void customerWithParcelCannotBeDeleted() {
        assertEquals(409, code("DELETE", "/api/customers/1", ""));
    }
    @Test public void customerDeleteWorks() {
        code("POST", "/api/customers", "{\"name\":\"Bob\",\"phone\":\"555\"}");
        assertEquals(200, code("DELETE", "/api/customers/2", ""));
        assertEquals(404, code("GET", "/api/customers/2", ""));
    }

    // ---- Courier ----
    @Test public void courierIsActiveByDefault() {
        assertTrue(s.handle("GET", "/api/couriers/1", "").body().contains("\"active\":true"));
    }
    @Test public void inactiveCourierCannotGetDelivery() {
        code("PUT", "/api/couriers/1", "{\"active\":false}");
        assertEquals(400, newDelivery());
    }
    @Test public void courierWithDeliveryCannotBeDeleted() {
        newDelivery();
        assertEquals(409, code("DELETE", "/api/couriers/1", ""));
    }

    // ---- Parcel ----
    @Test public void parcelNeedsExistingCustomer() {
        assertEquals(400, code("POST", "/api/packages", "{\"customerId\":42,\"description\":\"X\",\"weightKg\":1}"));
    }
    @Test public void parcelWeightMustBePositive() {
        assertEquals(400, code("POST", "/api/packages", "{\"customerId\":1,\"description\":\"X\",\"weightKg\":0}"));
        assertEquals(400, code("POST", "/api/packages", "{\"customerId\":1,\"description\":\"X\",\"weightKg\":-3}"));
    }
    @Test public void parcelWithDeliveryCannotBeDeleted() {
        newDelivery();
        assertEquals(409, code("DELETE", "/api/packages/1", ""));
    }

    // ---- Delivery and status history ----
    @Test public void newDeliveryIsPendingAndHasHistory() {
        assertEquals(201, newDelivery());
        assertTrue(s.handle("GET", "/api/deliveries/1", "").body().contains("\"status\":\"Pending\""));
        assertEquals(1, s.history.findAll().size());
    }
    @Test public void statusFollowsNormalFlow() {
        newDelivery();
        assertEquals(200, code("PUT", "/api/deliveries/1", "{\"status\":\"In Transit\"}"));
        assertEquals(200, code("PUT", "/api/deliveries/1", "{\"status\":\"Delivered\"}"));
        assertEquals(3, s.history.findAll().size());
    }
    @Test public void cannotSkipFromPendingToDelivered() {
        newDelivery();
        assertEquals(409, code("PUT", "/api/deliveries/1", "{\"status\":\"Delivered\"}"));
    }
    @Test public void deliveredCannotChangeAgain() {
        newDelivery();
        code("PUT", "/api/deliveries/1", "{\"status\":\"In Transit\"}");
        code("PUT", "/api/deliveries/1", "{\"status\":\"Delivered\"}");
        assertEquals(409, code("PUT", "/api/deliveries/1", "{\"status\":\"Cancelled\"}"));
    }
    @Test public void unknownStatusRejected() {
        newDelivery();
        assertEquals(400, code("PUT", "/api/deliveries/1", "{\"status\":\"Lost\"}"));
    }
    @Test public void deletingDeliveryRemovesItsHistory() {
        newDelivery();
        code("PUT", "/api/deliveries/1", "{\"status\":\"In Transit\"}");
        assertEquals(200, code("DELETE", "/api/deliveries/1", ""));
        assertEquals(0, s.history.findAll().size());
    }
    @Test public void historyIsReadOnly() {
        newDelivery();
        assertEquals(200, code("GET", "/api/history", ""));
        assertEquals(405, code("POST", "/api/history", "{}"));
        assertEquals(405, code("DELETE", "/api/history/1", ""));
    }

    // ---- other ----
    @Test public void badJsonGives400() {
        assertEquals(400, code("POST", "/api/customers", "{name:}"));
    }
    @Test public void statsCountDeliveries() {
        newDelivery();
        String b = s.handle("GET", "/api/stats", "").body();
        assertTrue(b.contains("\"deliveries\":1"));
        assertTrue(b.contains("\"Pending\":1"));
    }

    // ---- real HTTP server ----
    @Test public void httpServerWorks() throws Exception {
        HttpServer server = Main.start(0, s);
        try {
            int port = server.getAddress().getPort();
            HttpClient c = HttpClient.newHttpClient();
            HttpResponse<String> r = c.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/customers")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, r.statusCode());
            assertTrue(r.body().contains("Anna"));
            HttpResponse<String> page = c.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, page.statusCode());
        } finally {
            server.stop(0);
        }
    }
}
