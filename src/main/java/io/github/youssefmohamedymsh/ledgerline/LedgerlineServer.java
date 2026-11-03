package io.github.youssefmohamedymsh.ledgerline;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Ledgerline: a small, fictional business-banking onboarding app used as the system under test.
 * Serves the web pages from /web on the classpath and a JSON API under /api.
 *
 * Run it on its own with `mvn compile exec:java` and open http://localhost:8080.
 */
public final class LedgerlineServer {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "svg", "image/svg+xml");

    private final HttpServer server;
    private final Onboarding onboarding = new Onboarding(Clock.systemUTC());

    private LedgerlineServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/api/", this::handleApi);
        server.createContext("/", this::handleStatic);
        server.setExecutor(Executors.newFixedThreadPool(8));
    }

    /** Starts the app on the given port (0 picks a free port) and returns it. */
    public static LedgerlineServer start(int port) throws IOException {
        LedgerlineServer s = new LedgerlineServer(port);
        s.server.start();
        return s;
    }

    public String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort() + "/";
    }

    public void stop() {
        server.stop(0);
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getProperty("port", "8080"));
        LedgerlineServer s = start(port);
        System.out.println("Ledgerline demo running at " + s.baseUrl());
    }

    // ---------- API ----------

    private void handleApi(HttpExchange ex) throws IOException {
        try {
            Object result = route(ex);
            send(ex, ex.getRequestMethod().equals("POST") && ex.getRequestURI().getPath().equals("/api/signup") ? 201 : 200,
                    result);
        } catch (Onboarding.OnboardingException e) {
            send(ex, e.status, e.body);
        } catch (NotFound e) {
            send(ex, 404, Map.of("message", "Not found"));
        } catch (Exception e) {
            send(ex, 400, Map.of("message", "Malformed request"));
        }
    }

    private Object route(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();

        switch (method + " " + path) {
            case "POST /api/signup": {
                Map<String, Object> b = body(ex);
                Onboarding.Application app = onboarding.signUp(
                        s(b, "email"), s(b, "firstName"), s(b, "lastName"), s(b, "companyName"));
                return Map.of("applicationId", app.id, "email", app.email);
            }
            case "GET /api/inbox": {
                return Map.of("emails", onboarding.inbox(query(ex).get("to")));
            }
            case "POST /api/verify": {
                Map<String, Object> b = body(ex);
                return Map.of("session", onboarding.verifyEmail(s(b, "token"), s(b, "password")));
            }
            case "POST /api/login": {
                Map<String, Object> b = body(ex);
                return Map.of("session", onboarding.logIn(s(b, "email"), s(b, "password")));
            }
            case "GET /api/application": {
                return onboarding.view(authenticated(ex));
            }
            case "PUT /api/application/business": {
                Onboarding.Application app = authenticated(ex);
                onboarding.saveBusiness(app, body(ex));
                return onboarding.view(app);
            }
            case "PUT /api/application/owners": {
                Onboarding.Application app = authenticated(ex);
                Map<String, Object> b = body(ex);
                List<Map<String, Object>> owners = JSON.convertValue(b.get("owners"), new TypeReference<>() {
                });
                onboarding.saveOwners(app, owners);
                return onboarding.view(app);
            }
            case "PUT /api/application/document": {
                Onboarding.Application app = authenticated(ex);
                onboarding.saveDocument(app, body(ex));
                return onboarding.view(app);
            }
            case "POST /api/application/submit": {
                Onboarding.Application app = authenticated(ex);
                onboarding.submit(app);
                return onboarding.view(app);
            }
            case "POST /api/team/invite": {
                Onboarding.Application app = authenticated(ex);
                return onboarding.inviteTeam(app, s(body(ex), "csv"));
            }
            default:
                throw new NotFound();
        }
    }

    private Onboarding.Application authenticated(HttpExchange ex) {
        String header = ex.getRequestHeaders().getFirst("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
        return onboarding.forSession(token);
    }

    private static Map<String, Object> body(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            byte[] bytes = in.readAllBytes();
            if (bytes.length == 0) {
                return new LinkedHashMap<>();
            }
            return JSON.readValue(bytes, new TypeReference<>() {
            });
        }
    }

    private static Map<String, String> query(HttpExchange ex) {
        Map<String, String> q = new LinkedHashMap<>();
        String raw = ex.getRequestURI().getRawQuery();
        if (raw == null) {
            return q;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                q.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return q;
    }

    private static String s(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? null : v.toString();
    }

    private static void send(HttpExchange ex, int status, Object payload) throws IOException {
        byte[] bytes = JSON.writeValueAsBytes(payload);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    // ---------- Static pages ----------

    private void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }
        if (path.contains("..")) {
            ex.sendResponseHeaders(400, -1);
            return;
        }
        try (InputStream in = LedgerlineServer.class.getResourceAsStream("/web" + path)) {
            if (in == null) {
                byte[] msg = "Not found".getBytes(StandardCharsets.UTF_8);
                ex.sendResponseHeaders(404, msg.length);
                ex.getResponseBody().write(msg);
                ex.close();
                return;
            }
            byte[] bytes = in.readAllBytes();
            String ext = path.substring(path.lastIndexOf('.') + 1);
            ex.getResponseHeaders().set("Content-Type", CONTENT_TYPES.getOrDefault(ext, "application/octet-stream"));
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = ex.getResponseBody()) {
                out.write(bytes);
            }
        }
    }

    private static final class NotFound extends RuntimeException {
    }
}
