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
    // Session matrices: Map<SessionID, Map<String, double[][]>>
    private final Map<String, Map<String, double[][]>> sessionMatrices = new ConcurrentHashMap<>();
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
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-Session-ID");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                try (InputStream is = exchange.getRequestBody()) {
                    String sessionId = exchange.getRequestHeaders().getFirst("X-Session-ID");
                    if (sessionId == null || sessionId.isEmpty()) {
                        sessionId = "default";
                    }

                    String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    Map<String, Object> requestMap = objectMapper.readValue(requestBody, HashMap.class);
                    String operation = (String) requestMap.get("operation");

                    if (operation == null) {
                        sendResponse(exchange, 400, "{\"error\":\"Operation field is missing.\"}");
                        return;
                    }

                    Map<String, Object> responseMap = new HashMap<>();

                    if (operation.equals("store")) {
                        String name = (String) requestMap.get("name");
                        double[][] A = getMatrixFromParam(requestMap.get("matrix"), sessionId);
                        if (name == null || A == null) {
                            sendResponse(exchange, 400, "{\"error\":\"name and matrix are required for store.\"}");
                            return;
                        }
                        sessionMatrices.computeIfAbsent(sessionId, id -> new ConcurrentHashMap<>()).put(name, A);
                        responseMap.put("status", "success");
                        responseMap.put("storedName", name);
                    } else if (operation.equals("preset")) {
                        String name = (String) requestMap.get("name");
                        int rows = ((Number) requestMap.get("rows")).intValue();
                        int cols = ((Number) requestMap.get("cols")).intValue();
                        List<Number> paramsList = (List<Number>) requestMap.get("params");
                        double[] params = null;
                        if (paramsList != null) {
                            params = new double[paramsList.size()];
                            for (int i = 0; i < paramsList.size(); i++) {
                                params[i] = paramsList.get(i).doubleValue();
                            }
                        }
                        double[][] result = MatrixMath.generatePreset(name, rows, cols, params);
                        responseMap.put("result", result);
                    } else if (operation.equals("add")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] B = getMatrixFromParam(requestMap.get("matrixB"), sessionId);
                        double[][] result = MatrixMath.add(A, B);
                        responseMap.put("result", result);
                    } else if (operation.equals("subtract")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] B = getMatrixFromParam(requestMap.get("matrixB"), sessionId);
                        double[][] result = MatrixMath.subtract(A, B);
                        responseMap.put("result", result);
                    } else if (operation.equals("multiply")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] B = getMatrixFromParam(requestMap.get("matrixB"), sessionId);
                        double[][] result = MatrixMath.multiply(A, B);
                        responseMap.put("result", result);
                    } else if (operation.equals("scalarMultiply")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double k = ((Number) requestMap.get("scalar")).doubleValue();
                        double[][] result = MatrixMath.scalarMultiply(A, k);
                        responseMap.put("result", result);
                    } else if (operation.equals("hadamardProduct")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] B = getMatrixFromParam(requestMap.get("matrixB"), sessionId);
                        double[][] result = MatrixMath.hadamardProduct(A, B);
                        responseMap.put("result", result);
                    } else if (operation.equals("hadamardDivide")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] B = getMatrixFromParam(requestMap.get("matrixB"), sessionId);
                        double[][] result = MatrixMath.hadamardDivide(A, B);
                        responseMap.put("result", result);
                    } else if (operation.equals("power")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        int p = ((Number) requestMap.get("power")).intValue();
                        double[][] result = MatrixMath.power(A, p);
                        responseMap.put("result", result);
                    } else if (operation.equals("kronecker")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] B = getMatrixFromParam(requestMap.get("matrixB"), sessionId);
                        double[][] result = MatrixMath.kroneckerProduct(A, B);
                        responseMap.put("result", result);
                    } else if (operation.equals("directSum")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] B = getMatrixFromParam(requestMap.get("matrixB"), sessionId);
                        double[][] result = MatrixMath.directSum(A, B);
                        responseMap.put("result", result);
                    } else if (operation.equals("transpose")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] result = MatrixMath.transpose(A);
                        responseMap.put("result", result);
                    } else if (operation.equals("determinant")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double result = MatrixMath.determinant(A);
                        responseMap.put("result", result);
                    } else if (operation.equals("minors")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] result = MatrixMath.minors(A);
                        responseMap.put("result", result);
                    } else if (operation.equals("cofactors")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] result = MatrixMath.cofactors(A);
                        responseMap.put("result", result);
                    } else if (operation.equals("adjugate")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] result = MatrixMath.adjugate(A);
                        responseMap.put("result", result);
                    } else if (operation.equals("permanent")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double result = MatrixMath.permanent(A);
                        responseMap.put("result", result);
                    } else if (operation.equals("cofactorExpansionSteps")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        int index = ((Number) requestMap.get("index")).intValue();
                        boolean isRow = (Boolean) requestMap.get("isRow");
                        Map<String, Object> result = MatrixMath.cofactorExpansionSteps(A, index, isRow);
                        responseMap.putAll(result);
                    } else if (operation.equals("inverse")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] result = MatrixMath.invert(A);
                        responseMap.put("result", result);
                    } else if (operation.equals("leftInverse")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] result = MatrixMath.leftInverse(A);
                        responseMap.put("result", result);
                    } else if (operation.equals("rightInverse")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] result = MatrixMath.rightInverse(A);
                        responseMap.put("result", result);
                    } else if (operation.equals("ref")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] result = MatrixMath.ref(A);
                        responseMap.put("result", result);
                    } else if (operation.equals("rref")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] result = MatrixMath.rref(A);
                        responseMap.put("result", result);
                    } else if (operation.equals("rrefSteps")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        Map<String, Object> result = MatrixMath.rrefWithSteps(A);
                        responseMap.putAll(result);
                    } else if (operation.equals("rank")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        int result = MatrixMath.rank(A);
                        responseMap.put("result", result);
                    } else if (operation.equals("nullity")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        int result = MatrixMath.nullity(A);
                        responseMap.put("result", result);
                    } else if (operation.equals("subspaces")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        responseMap.put("rowSpace", MatrixMath.rowSpace(A));
                        responseMap.put("columnSpace", MatrixMath.columnSpace(A));
                        responseMap.put("nullSpace", MatrixMath.nullSpace(A));
                        responseMap.put("leftNullSpace", MatrixMath.leftNullSpace(A));
                    } else if (operation.equals("solve") || operation.equals("solveLU")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[] b = getVectorFromParam(requestMap.get("vectorB"));
                        double[] x = MatrixMath.solveLU(A, b);
                        List<Double> xList = new ArrayList<>();
                        for (double val : x) xList.add(val);
                        responseMap.put("solution", xList);
                    } else if (operation.equals("solveCramer")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[] b = getVectorFromParam(requestMap.get("vectorB"));
                        double[] x = MatrixMath.solveCramer(A, b);
                        List<Double> xList = new ArrayList<>();
                        for (double val : x) xList.add(val);
                        responseMap.put("solution", xList);
                    } else if (operation.equals("solveLeastSquares")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[] b = getVectorFromParam(requestMap.get("vectorB"));
                        double[] x = MatrixMath.solveLeastSquares(A, b);
                        List<Double> xList = new ArrayList<>();
                        for (double val : x) xList.add(val);
                        responseMap.put("solution", xList);
                    } else if (operation.equals("decomposeLU")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        Map<String, double[][]> decomp = MatrixMath.decomposeLU(A);
                        responseMap.put("L", decomp.get("L"));
                        responseMap.put("U", decomp.get("U"));
                        responseMap.put("P", decomp.get("P"));
                    } else if (operation.equals("decomposeQR")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        Map<String, double[][]> decomp = MatrixMath.decomposeQR(A);
                        responseMap.put("Q", decomp.get("Q"));
                        responseMap.put("R", decomp.get("R"));
                    } else if (operation.equals("decomposeCholesky")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] L = MatrixMath.decomposeCholesky(A);
                        responseMap.put("L", L);
                    } else if (operation.equals("characteristicPolynomial")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[] coeff = MatrixMath.characteristicPolynomial(A);
                        responseMap.put("result", coeff);
                    } else if (operation.equals("eigenvalues")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        MatrixMath.Complex[] ev = MatrixMath.eigenvalues(A);
                        List<Map<String, Double>> list = new ArrayList<>();
                        for (MatrixMath.Complex val : ev) {
                            Map<String, Double> cMap = new HashMap<>();
                            cMap.put("re", val.re);
                            cMap.put("im", val.im);
                            list.add(cMap);
                        }
                        responseMap.put("result", list);
                    } else if (operation.equals("eigenvectors")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double lambda = ((Number) requestMap.get("lambda")).doubleValue();
                        List<double[]> list = MatrixMath.eigenvectors(A, lambda);
                        responseMap.put("result", list);
                    } else if (operation.equals("eigen")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        MatrixMath.Complex[] ev = MatrixMath.eigenvalues(A);
                        List<Map<String, Double>> listEV = new ArrayList<>();
                        List<List<double[]>> listVectors = new ArrayList<>();
                        for (MatrixMath.Complex val : ev) {
                            Map<String, Double> cMap = new HashMap<>();
                            cMap.put("re", val.re);
                            cMap.put("im", val.im);
                            listEV.add(cMap);
                            if (Math.abs(val.im) < 1e-9) {
                                listVectors.add(MatrixMath.eigenvectors(A, val.re));
                            } else {
                                listVectors.add(new ArrayList<>());
                            }
                        }
                        responseMap.put("eigenvalues", listEV);
                        responseMap.put("eigenvectors", listVectors);
                    } else if (operation.equals("properties")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        responseMap.put("trace", MatrixMath.trace(A));
                        responseMap.put("norms", MatrixMath.norms(A));
                        responseMap.put("classification", MatrixMath.classifyMatrix(A));
                    } else {
                        sendResponse(exchange, 400, "{\"error\":\"Unknown matrix operation: " + operation + "\"}");
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

        private double[][] getMatrixFromParam(Object param, String sessionId) {
            if (param == null) return null;
            if (param instanceof String) {
                String name = (String) param;
                Map<String, double[][]> matrices = sessionMatrices.get(sessionId);
                if (matrices != null && matrices.containsKey(name)) {
                    return matrices.get(name);
                }
                throw new IllegalArgumentException("Stored matrix '" + name + "' not found in session.");
            } else if (param instanceof List) {
                List<List<Number>> list = (List<List<Number>>) param;
                int r = list.size();
                int c = list.get(0).size();
                double[][] res = new double[r][c];
                for (int i = 0; i < r; i++) {
                    for (int j = 0; j < c; j++) {
                        res[i][j] = list.get(i).get(j).doubleValue();
                    }
                }
                return res;
            }
            throw new IllegalArgumentException("Invalid matrix parameter type.");
        }

        private double[] getVectorFromParam(Object param) {
            if (param == null) return null;
            if (param instanceof List) {
                List<Number> list = (List<Number>) param;
                double[] res = new double[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    res[i] = list.get(i).doubleValue();
                }
                return res;
            }
            throw new IllegalArgumentException("Invalid vector parameter.");
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
