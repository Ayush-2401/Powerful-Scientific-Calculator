package com.scientificcalculator.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scientificcalculator.engine.CalculatorEngine;
import com.scientificcalculator.engine.ExpressionTokenizer;
import com.scientificcalculator.engine.InfixToPostfixConverter;
import com.scientificcalculator.engine.PostfixEvaluator;
import com.scientificcalculator.engine.MatrixMath;
import com.scientificcalculator.engine.UnitConverter;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        server.createContext("/api/graph", new GraphHandler());
        server.createContext("/api/matrix", new MatrixHandler());
        server.createContext("/api/convert", new ConvertHandler());
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

    class GraphHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                try (InputStream is = exchange.getRequestBody()) {
                    String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    Map<String, Object> requestMap = objectMapper.readValue(requestBody, HashMap.class);
                    String expression = (String) requestMap.get("expression");
                    double xmin = ((Number) requestMap.get("xmin")).doubleValue();
                    double xmax = ((Number) requestMap.get("xmax")).doubleValue();
                    double step = ((Number) requestMap.get("step")).doubleValue();

                    if (expression == null || step <= 0 || xmin > xmax) {
                        sendResponse(exchange, 400, "{\"error\":\"Invalid graphing parameters.\"}");
                        return;
                    }

                    List<String> infixTokens = ExpressionTokenizer.tokenize(expression);
                    List<String> postfixTokens = InfixToPostfixConverter.convert(infixTokens, "x");
                    PostfixEvaluator evaluator = new PostfixEvaluator(false); // Evaluate in radian mode for graphing

                    List<Map<String, Object>> points = new ArrayList<>();
                    for (double x = xmin; x <= xmax; x += step) {
                        Map<String, Object> point = new HashMap<>();
                        point.put("x", x);
                        try {
                            BigDecimal yVal = evaluator.evaluate(postfixTokens, "x", BigDecimal.valueOf(x));
                            point.put("y", yVal.doubleValue());
                        } catch (Exception e) {
                            point.put("y", null); // Set y to null for graphing gap (e.g. division by zero, log of negative)
                        }
                        points.add(point);
                    }

                    Map<String, Object> responseMap = new HashMap<>();
                    responseMap.put("points", points);
                    sendResponse(exchange, 200, objectMapper.writeValueAsString(responseMap));
                } catch (Exception e) {
                    e.printStackTrace();
                    sendResponse(exchange, 500, "{\"error\":\"Internal server error: " + e.getMessage() + "\"}");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
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

    class MatrixHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                try (InputStream is = exchange.getRequestBody()) {
                    String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    Map<String, Object> requestMap = objectMapper.readValue(requestBody, HashMap.class);
                    String operation = (String) requestMap.get("operation");

                    if (operation == null) {
                        sendResponse(exchange, 400, "{\"error\":\"Operation field is missing.\"}");
                        return;
                    }

                    Map<String, Object> responseMap = new HashMap<>();

                    if (operation.equals("solve")) {
                        List<List<Number>> matrixAList = (List<List<Number>>) requestMap.get("matrixA");
                        List<Number> vectorBList = (List<Number>) requestMap.get("vectorB");
                        if (matrixAList == null || vectorBList == null) {
                            sendResponse(exchange, 400, "{\"error\":\"matrixA and vectorB are required for solve.\"}");
                            return;
                        }
                        int n = vectorBList.size();
                        double[][] A = new double[n][n];
                        double[] b = new double[n];
                        for (int i = 0; i < n; i++) {
                            b[i] = vectorBList.get(i).doubleValue();
                            for (int j = 0; j < n; j++) {
                                A[i][j] = matrixAList.get(i).get(j).doubleValue();
                            }
                        }
                        double[] x = MatrixMath.solveSystem(A, b);
                        List<Double> xList = new ArrayList<>();
                        for (double val : x) xList.add(val);
                        responseMap.put("solution", xList);
                    } else {
                        List<List<Number>> matrixAList = (List<List<Number>>) requestMap.get("matrixA");
                        if (matrixAList == null) {
                            sendResponse(exchange, 400, "{\"error\":\"matrixA is required.\"}");
                            return;
                        }
                        int r1 = matrixAList.size();
                        int c1 = matrixAList.get(0).size();
                        double[][] A = new double[r1][c1];
                        for (int i = 0; i < r1; i++) {
                            for (int j = 0; j < c1; j++) {
                                A[i][j] = matrixAList.get(i).get(j).doubleValue();
                            }
                        }

                        if (operation.equals("multiply")) {
                            List<List<Number>> matrixBList = (List<List<Number>>) requestMap.get("matrixB");
                            if (matrixBList == null) {
                                sendResponse(exchange, 400, "{\"error\":\"matrixB is required for multiplication.\"}");
                                return;
                            }
                            int r2 = matrixBList.size();
                            int c2 = matrixBList.get(0).size();
                            double[][] B = new double[r2][c2];
                            for (int i = 0; i < r2; i++) {
                                for (int j = 0; j < c2; j++) {
                                    B[i][j] = matrixBList.get(i).get(j).doubleValue();
                                }
                            }
                            double[][] result = MatrixMath.multiply(A, B);
                            responseMap.put("result", result);
                        } else if (operation.equals("transpose")) {
                            double[][] result = MatrixMath.transpose(A);
                            responseMap.put("result", result);
                        } else if (operation.equals("determinant")) {
                            double result = MatrixMath.determinant(A);
                            responseMap.put("result", result);
                        } else if (operation.equals("inverse")) {
                            double[][] result = MatrixMath.invert(A);
                            responseMap.put("result", result);
                        } else {
                            sendResponse(exchange, 400, "{\"error\":\"Unknown matrix operation: " + operation + "\"}");
                            return;
                        }
                    }

                    sendResponse(exchange, 200, objectMapper.writeValueAsString(responseMap));
                } catch (Exception e) {
                    e.printStackTrace();
                    sendResponse(exchange, 500, "{\"error\":\"Internal server error: " + e.getMessage() + "\"}");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
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

    class ConvertHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                try (InputStream is = exchange.getRequestBody()) {
                    String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    Map<String, String> requestMap = objectMapper.readValue(requestBody, HashMap.class);
                    String type = requestMap.get("type");
                    String value = requestMap.get("value");
                    String from = requestMap.get("from");
                    String to = requestMap.get("to");

                    if (type == null || value == null || from == null || to == null) {
                        sendResponse(exchange, 400, "{\"error\":\"Missing required parameters: type, value, from, to.\"}");
                        return;
                    }

                    Map<String, String> responseMap = new HashMap<>();

                    if (type.equals("unit")) {
                        String category = requestMap.get("category");
                        if (category == null) {
                            sendResponse(exchange, 400, "{\"error\":\"Category is required for unit conversions.\"}");
                            return;
                        }
                        BigDecimal decimalVal = new BigDecimal(value);
                        BigDecimal converted = UnitConverter.convertUnit(category, decimalVal, from, to);
                        responseMap.put("result", converted.stripTrailingZeros().toPlainString());
                    } else if (type.equals("base")) {
                        String converted = UnitConverter.convertBase(value, from, to);
                        responseMap.put("result", converted);
                    } else {
                        sendResponse(exchange, 400, "{\"error\":\"Unknown conversion type: " + type + "\"}");
                        return;
                    }

                    sendResponse(exchange, 200, objectMapper.writeValueAsString(responseMap));
                } catch (Exception e) {
                    e.printStackTrace();
                    sendResponse(exchange, 500, "{\"error\":\"Internal server error: " + e.getMessage() + "\"}");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
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
