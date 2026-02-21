package com.duntale.dungeongen.rest;

import com.duntale.dungeongen.config.DungeonConfig;
import com.duntale.dungeongen.generator.GenerationOrchestrator;
import com.duntale.dungeongen.generator.GenerationResult;
import com.duntale.dungeongen.util.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Lightweight HTTP REST server exposing dungeon generation endpoints.
 * Uses the built-in JDK {@link HttpServer} — no external dependencies.
 *
 * <h2>Endpoints:</h2>
 * <pre>{@code
 *   GET  /health   → {"status": "ok", "version": "1.0.0"}
 *   POST /generate → accepts DungeonConfig JSON, returns generation stats
 * }</pre>
 *
 * @since 1.0.0
 */
public class HttpRestServer {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int FUTURE_TIMEOUT_SECONDS = 60;
    private static final String VERSION = "1.0.0";

    // ============================================
    // Fields
    // ============================================

    private final GenerationOrchestrator orchestrator;
    private final int port;
    private HttpServer server;

    // ============================================
    // Constructor
    // ============================================

    /**
     * Create a REST server wired to the given generation orchestrator.
     *
     * @param orchestrator the dungeon generation orchestrator
     * @param port         the TCP port to listen on
     */
    public HttpRestServer(@Nonnull GenerationOrchestrator orchestrator, int port) {
        this.orchestrator = orchestrator;
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
                Thread t = new Thread(r, "DungeonGen-REST");
                t.setDaemon(true);
                return t;
            }));

            registerEndpoints();
            server.start();

            LOGGER.atInfo().log("[DungeonGen-REST] HTTP server started on port %s", port);
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("[DungeonGen-REST] Failed to start HTTP server on port %s", port);
        }
    }

    /**
     * Stop the HTTP server gracefully.
     */
    public void stop() {
        if (server != null) {
            server.stop(1);
            LOGGER.atInfo().log("[DungeonGen-REST] HTTP server stopped");
        }
    }

    // ============================================
    // Endpoint Registration
    // ============================================

    private void registerEndpoints() {
        server.createContext("/health", this::handleHealth);
        server.createContext("/generate", this::handleGenerate);
    }

    // ============================================
    // Handlers
    // ============================================

    private void handleHealth(@Nonnull HttpExchange exchange) throws IOException {
        if (!requireGet(exchange)) return;
        sendJson(exchange, 200, "{\"status\": \"ok\", \"version\": \"" + VERSION + "\"}");
    }

    private void handleGenerate(@Nonnull HttpExchange exchange) throws IOException {
        if (!requirePost(exchange)) return;

        Map<String, Object> body = parseJsonBody(exchange);
        if (body == null) {
            sendJson(exchange, 400, errorJson("Invalid JSON body"));
            return;
        }

        try {
            DungeonConfig config = DungeonConfig.fromJson(body);

            LOGGER.atInfo().log("[DungeonGen-REST] Generation request: seed=%s, preset=%s",
                config.seed(), config.preset());

            // Run /clear command to wipe the area before generation
            if (config.clear()) {
                World world = Universe.get().getWorld(config.worldName());
                if (world != null) {
                    CompletableFuture<Void> clearFuture = new CompletableFuture<>();
                    world.execute(() -> {
                        PlayerRef clearPlayer = Universe.get().getPlayerByUsername("zki", NameMatching.EXACT_IGNORE_CASE);
                        if (clearPlayer != null) {
                            String clearCmd = "clear -300 0 -300 300 130 300";
                            LOGGER.atInfo().log("[DungeonGen-REST] Running clear command: /%s", clearCmd);
                            CommandManager.get().handleCommand(clearPlayer, clearCmd)
                                .whenComplete((v, ex) -> {
                                    if (ex != null) clearFuture.completeExceptionally(ex);
                                    else clearFuture.complete(null);
                                });
                        } else {
                            LOGGER.atWarning().log("[DungeonGen-REST] Cannot clear: player 'zki' not online");
                            clearFuture.complete(null);
                        }
                    });
                    clearFuture.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } else {
                    LOGGER.atWarning().log("[DungeonGen-REST] Cannot clear: world '%s' not found", config.worldName());
                }
            }

            GenerationResult result = orchestrator.generate(config)
                .get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            int httpStatus = result.assemblyError() != null ? 500 : 200;
            sendJson(exchange, httpStatus, result.toJson());
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[DungeonGen-REST] Generation failed");
            sendJson(exchange, 500, errorJson("Generation failed: " + e.getMessage()));
        }
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

    private boolean requirePost(@Nonnull HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, errorJson("POST required"));
            return false;
        }
        return true;
    }

    private boolean requireGet(@Nonnull HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, errorJson("GET required"));
            return false;
        }
        return true;
    }

    @Nullable
    private Map<String, Object> parseJsonBody(@Nonnull HttpExchange exchange) {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            return JsonParser.parseObject(body);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[DungeonGen-REST] Failed to parse JSON body");
            return null;
        }
    }

    @Nonnull
    private static String errorJson(@Nonnull String message) {
        return "{\"error\": \"" + message.replace("\"", "\\\"") + "\"}";
    }
}
