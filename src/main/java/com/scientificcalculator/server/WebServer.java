package com.scientificcalculator.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scientificcalculator.engine.CalculatorEngine;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class WebServer {

    private static final int PORT = 8080;
    // Session storage: Map<SessionID, CalculatorEngine>
    private final Map<String, CalculatorEngine> sessionEngines = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        new WebServer().start();
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/calculate", new CalculateHandler());
        server.setExecutor(Executors.newCachedThreadPool()); // Use cached thread pool for better concurrency
        server.start();
        System.out.println("Server started on port " + PORT);
    }

    class CalculateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Handle CORS for all requests
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-Session-ID");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                try (InputStream is = exchange.getRequestBody()) {
                    // 1. Get Session ID from Header
                    String sessionId = exchange.getRequestHeaders().getFirst("X-Session-ID");

                    // If no session ID provided, generate one (or treat as error depending on
                    // policy)
                    // For this app, we'll require the frontend to generate/store it, or we return a
                    // new one.
                    // Simpler approach: If missing, create a new engine but warn.
                    if (sessionId == null || sessionId.isEmpty()) {
                        System.out.println("Warning: No Session ID provided. Creating temporary session.");
                        sessionId = UUID.randomUUID().toString();
                    }

                    // 2. Get or Create Engine for this Session
                    CalculatorEngine engine = sessionEngines.computeIfAbsent(sessionId, id -> {
                        System.out.println("Creating new CalculatorEngine for session: " + id);
                        return new CalculatorEngine();
                    });

                    // 3. Parse Request
                    String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    Map<String, String> requestMap = objectMapper.readValue(requestBody, HashMap.class);
                    String command = requestMap.get("command");

                    if (command == null) {
                        sendResponse(exchange, 400, "{\"error\":'command' field is missing}");
                        return;
                    }

                    // 4. Process Command
                    String[] displayUpdates = engine.processCommand(command);

                    // 5. Prepare Response
                    Map<String, String> responseMap = new HashMap<>();
                    responseMap.put("primaryDisplay", displayUpdates[0]);
                    responseMap.put("secondaryDisplay", displayUpdates[1]);
                    // Echo back the session ID so the client can store it if it was new (optional,
                    // but good practice)
                    responseMap.put("sessionId", sessionId);

                    String jsonResponse = objectMapper.writeValueAsString(responseMap);

                    sendResponse(exchange, 200, jsonResponse);

                } catch (Exception e) {
                    e.printStackTrace();
                    sendResponse(exchange, 500, "{\"error\":'Internal server error: " + e.getMessage() + "'}");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":'Method not allowed'}");
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
}
