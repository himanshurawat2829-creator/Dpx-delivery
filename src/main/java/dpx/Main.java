package dpx;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/** Starts the web server: REST API under /api and the web page at /. */
public class Main {

    public static HttpServer start(int port, DpxService service) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", ex -> {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            DpxService.Response r = service.handle(ex.getRequestMethod(), ex.getRequestURI().getPath(), body);
            send(ex, r.status(), "application/json", r.body().getBytes(StandardCharsets.UTF_8));
        });
        server.createContext("/", ex -> {
            try (InputStream in = Main.class.getResourceAsStream("/static/index.html")) {
                if (in == null) send(ex, 404, "text/plain", "index.html not found".getBytes());
                else send(ex, 200, "text/html; charset=utf-8", in.readAllBytes());
            }
        });
        server.start();
        return server;
    }

    private static void send(HttpExchange ex, int status, String type, byte[] bytes) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        DpxService service = new DpxService();
        service.loadSampleData();
        start(port, service);
        System.out.println("DPX is running: http://localhost:" + port);
    }
}
