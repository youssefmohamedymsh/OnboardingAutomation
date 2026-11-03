package io.github.youssefmohamedymsh.kyc.support;

import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import io.github.youssefmohamedymsh.ledgerline.LedgerlineServer;
import io.restassured.RestAssured;

/**
 * Starts the bundled Ledgerline demo app once for the whole run, on a free port.
 * Pass -DbaseUrl=http://host:port/ to test an instance that's already running instead.
 */
public final class TestEnvironment {

    private static LedgerlineServer server;
    private static String baseUrl;

    private TestEnvironment() {
    }

    @BeforeAll
    public static void startApp() throws Exception {
        String external = System.getProperty("baseUrl");
        if (external != null && !external.isBlank()) {
            baseUrl = external.endsWith("/") ? external : external + "/";
        } else {
            server = LedgerlineServer.start(0);
            baseUrl = server.baseUrl();
        }
        RestAssured.baseURI = baseUrl;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterAll
    public static void stopApp() {
        if (server != null) {
            server.stop();
        }
    }

    public static String baseUrl() {
        return baseUrl;
    }

    public static String url(String path) {
        return baseUrl + (path.startsWith("/") ? path.substring(1) : path);
    }
}
