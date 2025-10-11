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
import java.util.concurrent.Executors;

public class WebServer {

    private static final int PORT = 8080;
    private final CalculatorEngine engine = new CalculatorEngine();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        new WebServer().start();
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/calculate", new CalculateHandler());
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        System.out.println("Server started on port " + PORT);
    }

    class CalculateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod()) || "OPTIONS".equals(exchange.getRequestMethod())) {
                // Set CORS headers for all responses
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); // Allow any origin
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

                // Respond to preflight OPTIONS request
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1); // No Content
                    return;
                }

                try (InputStream is = exchange.getRequestBody()) {
                    // Read the request body
                    String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                    // Parse the command from the JSON request
                    Map<String, String> requestMap = objectMapper.readValue(requestBody, HashMap.class);
                    String command = requestMap.get("command");

                    if (command == null) {
                        sendResponse(exchange, 400, "{\"error\":'command' field is missing}");
                        return;
                    }

                    // Process the command using the calculator engine
                    String[] displayUpdates = engine.processCommand(command);

                    // Prepare the JSON response
                    Map<String, String> responseMap = new HashMap<>();
                    responseMap.put("primaryDisplay", displayUpdates[0]);
                    responseMap.put("secondaryDisplay", displayUpdates[1]);
                    String jsonResponse = objectMapper.writeValueAsString(responseMap);

                    // Send the response
                    sendResponse(exchange, 200, jsonResponse);

                } catch (Exception e) {
                    e.printStackTrace();
                    sendResponse(exchange, 500, "{\"error\":'Internal server error'}");
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
