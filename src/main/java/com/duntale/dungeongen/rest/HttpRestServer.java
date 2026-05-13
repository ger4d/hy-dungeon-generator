package com.duntale.dungeongen.rest;

import com.hypixel.hytale.logger.HytaleLogger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Lightweight HTTP server exposing balancing asset export endpoints.
 * Uses the built-in JDK {@link HttpServer} — no external dependencies.
 *
 * <h2>Endpoints:</h2>
 * <pre>{@code
 *   GET /health
 *   GET /assets/summary
 *   GET /assets/weapons
 *   GET /assets/armor
 *   GET /assets/npcs
 *   GET /assets/balance-dataset
 * }</pre>
 *
 * @since 1.0.0
 */
public class HttpRestServer {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String VERSION = "1.0.0";

    // ============================================
    // Fields
    // ============================================

    private final BalanceAssetExportService exportService;
    private final int port;
    private HttpServer server;

    // ============================================
    // Constructor
    // ============================================

    /**
     * Create an asset API server wired to the given export service.
     *
     * @param exportService the balancing asset export service
     * @param port          the TCP port to listen on
     */
    public HttpRestServer(@Nonnull BalanceAssetExportService exportService, int port) {
        this.exportService = exportService;
        this.port = port;
    }

    // ============================================
    // Lifecycle
    // ============================================

    /**
     * Start the HTTP server and register all endpoints.
     */
    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "DungeonGen-AssetAPI");
                t.setDaemon(true);
                return t;
            }));

            registerEndpoints();
            server.start();

            LOGGER.atInfo().log("[DungeonGen-AssetAPI] HTTP server started on port %s", port);
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("[DungeonGen-AssetAPI] Failed to start HTTP server on port %s", port);
        }
    }

    /**
     * Stop the HTTP server gracefully.
     */
    public void stop() {
        if (server != null) {
            server.stop(1);
            LOGGER.atInfo().log("[DungeonGen-AssetAPI] HTTP server stopped");
        }
    }

    // ============================================
    // Endpoint Registration
    // ============================================

    private void registerEndpoints() {
        server.createContext("/health", this::handleHealth);
        server.createContext("/assets/summary", this::handleSummary);
        server.createContext("/assets/weapons", this::handleWeapons);
        server.createContext("/assets/armor", this::handleArmor);
        server.createContext("/assets/npcs", this::handleNpcs);
        server.createContext("/assets/balance-dataset", this::handleBalanceDataset);
    }

    // ============================================
    // Handlers
    // ============================================

    private void handleHealth(@Nonnull HttpExchange exchange) throws IOException {
        if (!requireGet(exchange)) return;
        sendJson(exchange, 200, "{\"status\": \"ok\", \"version\": \"" + VERSION + "\", \"mode\": \"balance-assets\"}");
    }

    private void handleSummary(@Nonnull HttpExchange exchange) throws IOException {
        if (!requireGet(exchange)) return;
        sendJson(exchange, 200, exportService.buildSummary().toString());
    }

    private void handleWeapons(@Nonnull HttpExchange exchange) throws IOException {
        if (!requireGet(exchange)) return;
        sendJson(exchange, 200, exportService.buildWeapons().toString());
    }

    private void handleArmor(@Nonnull HttpExchange exchange) throws IOException {
        if (!requireGet(exchange)) return;
        sendJson(exchange, 200, exportService.buildArmor().toString());
    }

    private void handleNpcs(@Nonnull HttpExchange exchange) throws IOException {
        if (!requireGet(exchange)) return;
        sendJson(exchange, 200, exportService.buildNpcs().toString());
    }

    private void handleBalanceDataset(@Nonnull HttpExchange exchange) throws IOException {
        if (!requireGet(exchange)) return;
        sendJson(exchange, 200, exportService.buildBalanceDataset().toString());
    }

    // ============================================
    // HTTP Utilities
    // ============================================

    private void sendJson(@Nonnull HttpExchange exchange, int statusCode,
                          @Nonnull String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private boolean requireGet(@Nonnull HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, errorJson("GET required"));
            return false;
        }
        return true;
    }

    @Nonnull
    private static String errorJson(@Nonnull String message) {
        return "{\"error\": \"" + message.replace("\"", "\\\"") + "\"}";
    }
}
