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

    public static void main(String[] args) throws Exception {
        new WebServer().start();
        Thread.currentThread().join();
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
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
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
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
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

                    // Translate UI operations to backend methods
                    String mappedOperation = operation;
                    if (operation.equals("scalar_multiply")) mappedOperation = "scalarMultiply";
                    else if (operation.equals("hadamard_product")) mappedOperation = "hadamardProduct";
                    else if (operation.equals("hadamard_division")) mappedOperation = "hadamardDivide";
                    else if (operation.equals("kronecker_product")) mappedOperation = "kronecker";
                    else if (operation.equals("direct_sum")) mappedOperation = "directSum";
                    else if (operation.equals("left_inverse")) mappedOperation = "leftInverse";
                    else if (operation.equals("right_inverse")) mappedOperation = "rightInverse";
                    else if (operation.equals("null_space")) mappedOperation = "nullSpace";
                    else if (operation.equals("column_space")) mappedOperation = "columnSpace";
                    else if (operation.equals("row_space")) mappedOperation = "rowSpace";
                    else if (operation.equals("left_null_space")) mappedOperation = "leftNullSpace";
                    else if (operation.equals("cramer")) mappedOperation = "solveCramer";
                    else if (operation.equals("lu_solve")) mappedOperation = "solveLU";
                    else if (operation.equals("least_squares")) mappedOperation = "solveLeastSquares";
                    else if (operation.equals("lu")) mappedOperation = "decomposeLU";
                    else if (operation.equals("qr")) mappedOperation = "decomposeQR";
                    else if (operation.equals("cholesky")) mappedOperation = "decomposeCholesky";
                    else if (operation.equals("char_poly")) mappedOperation = "characteristicPolynomial";
                    else if (operation.equals("auto_detect_type")) mappedOperation = "classifyMatrix";
                    else if (operation.equals("norms")) mappedOperation = "properties";

                    if (operation.equals("store")) {
                        String name = (String) requestMap.get("name");
                        double[][] A = getMatrixFromParam(requestMap.get("matrix"), sessionId);
                        if (name == null || A == null) {
                            sendResponse(exchange, 400, "{\"error\":\"name and matrix are required for store.\"}");
                            return;
                        }
                        sessionMatrices.computeIfAbsent(sessionId, id -> new ConcurrentHashMap<>()).put(name, A);
                        responseMap.put("result", "success");
                        responseMap.put("storedName", name);
                    } else if (mappedOperation.equals("preset")) {
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
                    } else if (mappedOperation.equals("add")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] B = getMatrixFromParam(requestMap.get("matrixB"), sessionId);
                        double[][] result = MatrixMath.add(A, B);
                        responseMap.put("result", result);
                    } else if (mappedOperation.equals("subtract")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] B = getMatrixFromParam(requestMap.get("matrixB"), sessionId);
                        double[][] result = MatrixMath.subtract(A, B);
                        responseMap.put("result", result);
                    } else if (mappedOperation.equals("multiply")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] B = getMatrixFromParam(requestMap.get("matrixB"), sessionId);
                        double[][] result = MatrixMath.multiply(A, B);
                        responseMap.put("result", result);
                    } else if (mappedOperation.equals("scalarMultiply")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double k = ((Number) requestMap.get("scalar")).doubleValue();
                        double[][] result = MatrixMath.scalarMultiply(A, k);
                        responseMap.put("result", result);
                    } else if (mappedOperation.equals("hadamardProduct")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] B = getMatrixFromParam(requestMap.get("matrixB"), sessionId);
                        double[][] result = MatrixMath.hadamardProduct(A, B);
                        responseMap.put("result", result);
                    } else if (mappedOperation.equals("hadamardDivide")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] B = getMatrixFromParam(requestMap.get("matrixB"), sessionId);
                        double[][] result = MatrixMath.hadamardDivide(A, B);
                        responseMap.put("result", result);
                    } else if (mappedOperation.equals("power")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        int p = ((Number) requestMap.get("power")).intValue();
                        double[][] result = MatrixMath.power(A, p);
                        responseMap.put("result", result);
                    } else if (mappedOperation.equals("kronecker")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] B = getMatrixFromParam(requestMap.get("matrixB"), sessionId);
                        double[][] result = MatrixMath.kroneckerProduct(A, B);
                        responseMap.put("result", result);
                    } else if (mappedOperation.equals("directSum")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] B = getMatrixFromParam(requestMap.get("matrixB"), sessionId);
                        double[][] result = MatrixMath.directSum(A, B);
                        responseMap.put("result", result);
                    } else if (mappedOperation.equals("transpose")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] result = MatrixMath.transpose(A);
                        responseMap.put("result", result);
                    } else if (mappedOperation.equals("determinant")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        boolean showSteps = requestMap.containsKey("showCofactorSteps") && (Boolean) requestMap.get("showCofactorSteps");
                        if (showSteps) {
                            Map<String, Object> stepsRes = MatrixMath.cofactorExpansionSteps(A, 0, true);
                            List<String> stepsStrings = new ArrayList<>();
                            stepsStrings.add("Cofactor Expansion along Row 1:");
                            List<Map<String, Object>> terms = (List<Map<String, Object>>) stepsRes.get("terms");
                            for (Map<String, Object> term : terms) {
                                int r = (Integer) term.get("row");
                                int c = (Integer) term.get("col");
                                double coeff = (Double) term.get("coefficient");
                                int sign = (Integer) term.get("sign");
                                double subDet = (Double) term.get("submatrixDeterminant");
                                double termValue = (Double) term.get("termValue");
                                
                                stepsStrings.add(String.format("Term (%d,%d): Coefficient = %s, Sign = %d", r + 1, c + 1, formatDouble(coeff), sign));
                                stepsStrings.add(String.format("  Submatrix M_%d%d:\n%s", r + 1, c + 1, formatMatrixToString((double[][]) term.get("submatrix"))));
                                stepsStrings.add(String.format("  det(M_%d%d) = %s", r + 1, c + 1, formatDouble(subDet)));
                                stepsStrings.add(String.format("  Value = %s * (%d) * %s = %s", formatDouble(coeff), sign, formatDouble(subDet), formatDouble(termValue)));
                            }
                            stepsStrings.add("Formula: " + stepsRes.get("formula"));
                            stepsStrings.add("Final Sum (Determinant) = " + formatDouble((Double) stepsRes.get("result")));
                            
                            Map<String, Object> resObj = new HashMap<>();
                            resObj.put("result", stepsRes.get("result"));
                            resObj.put("steps", stepsStrings);
                            responseMap.put("result", resObj);
                        } else {
                            responseMap.put("result", MatrixMath.determinant(A));
                        }
                    } else if (mappedOperation.equals("minors")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] result = MatrixMath.minors(A);
                        responseMap.put("result", result);
                    } else if (mappedOperation.equals("cofactors")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] result = MatrixMath.cofactors(A);
                        responseMap.put("result", result);
                    } else if (mappedOperation.equals("adjugate")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] result = MatrixMath.adjugate(A);
                        responseMap.put("result", result);
                    } else if (mappedOperation.equals("permanent")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double result = MatrixMath.permanent(A);
                        responseMap.put("result", result);
                    } else if (mappedOperation.equals("inverse")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        boolean showSteps = requestMap.containsKey("showRowOpSteps") && (Boolean) requestMap.get("showRowOpSteps");
                        if (showSteps) {
                            int n = A.length;
                            double[][] augmented = new double[n][2 * n];
                            for (int i = 0; i < n; i++) {
                                System.arraycopy(A[i], 0, augmented[i], 0, n);
                                augmented[i][n + i] = 1.0;
                            }
                            Map<String, Object> stepsRes = MatrixMath.rrefWithSteps(augmented);
                            List<Map<String, Object>> stepsList = (List<Map<String, Object>>) stepsRes.get("steps");
                            List<String> stepsStrings = new ArrayList<>();
                            stepsStrings.add("Gauss-Jordan elimination on augmented matrix [A | I]:");
                            for (Map<String, Object> step : stepsList) {
                                String desc = (String) step.get("description");
                                double[][] matState = (double[][]) step.get("matrix");
                                stepsStrings.add(desc + ":\n" + formatMatrixToString(matState));
                            }
                            
                            Map<String, Object> resObj = new HashMap<>();
                            resObj.put("result", MatrixMath.invert(A));
                            resObj.put("steps", stepsStrings);
                            responseMap.put("result", resObj);
                        } else {
                            responseMap.put("result", MatrixMath.invert(A));
                        }
                    } else if (operation.equals("pseudo_inverse")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        int r = A.length;
                        int c = A[0].length;
                        double[][] result;
                        if (r == c) {
                            try {
                                result = MatrixMath.invert(A);
                            } catch (Exception e) {
                                result = MatrixMath.leftInverse(A);
                            }
                        } else if (r > c) {
                            result = MatrixMath.leftInverse(A);
                        } else {
                            result = MatrixMath.rightInverse(A);
                        }
                        responseMap.put("result", result);
                    } else if (mappedOperation.equals("leftInverse")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] result = MatrixMath.leftInverse(A);
                        responseMap.put("result", result);
                    } else if (mappedOperation.equals("rightInverse")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] result = MatrixMath.rightInverse(A);
                        responseMap.put("result", result);
                    } else if (mappedOperation.equals("ref")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        boolean showSteps = requestMap.containsKey("showRowOpSteps") && (Boolean) requestMap.get("showRowOpSteps");
                        if (showSteps) {
                            // Generate RREF steps as a good indicator of REF process
                            Map<String, Object> stepsRes = MatrixMath.rrefWithSteps(A);
                            List<Map<String, Object>> stepsList = (List<Map<String, Object>>) stepsRes.get("steps");
                            List<String> stepsStrings = new ArrayList<>();
                            for (Map<String, Object> step : stepsList) {
                                String desc = (String) step.get("description");
                                double[][] matState = (double[][]) step.get("matrix");
                                stepsStrings.add(desc + ":\n" + formatMatrixToString(matState));
                            }
                            
                            Map<String, Object> resObj = new HashMap<>();
                            resObj.put("result", MatrixMath.ref(A));
                            resObj.put("steps", stepsStrings);
                            responseMap.put("result", resObj);
                        } else {
                            responseMap.put("result", MatrixMath.ref(A));
                        }
                    } else if (mappedOperation.equals("rref")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        boolean showSteps = requestMap.containsKey("showRowOpSteps") && (Boolean) requestMap.get("showRowOpSteps");
                        if (showSteps) {
                            Map<String, Object> stepsRes = MatrixMath.rrefWithSteps(A);
                            List<Map<String, Object>> stepsList = (List<Map<String, Object>>) stepsRes.get("steps");
                            List<String> stepsStrings = new ArrayList<>();
                            for (Map<String, Object> step : stepsList) {
                                String desc = (String) step.get("description");
                                double[][] matState = (double[][]) step.get("matrix");
                                stepsStrings.add(desc + ":\n" + formatMatrixToString(matState));
                            }
                            
                            Map<String, Object> resObj = new HashMap<>();
                            resObj.put("result", stepsRes.get("result"));
                            resObj.put("steps", stepsStrings);
                            responseMap.put("result", resObj);
                        } else {
                            responseMap.put("result", MatrixMath.rref(A));
                        }
                    } else if (mappedOperation.equals("rank")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        int result = MatrixMath.rank(A);
                        responseMap.put("result", result);
                    } else if (mappedOperation.equals("nullity")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        int result = MatrixMath.nullity(A);
                        responseMap.put("result", result);
                    } else if (mappedOperation.equals("nullSpace")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        List<double[]> basis = MatrixMath.nullSpace(A);
                        Map<String, Object> resObj = new HashMap<>();
                        resObj.put("basis", basis);
                        responseMap.put("result", resObj);
                    } else if (mappedOperation.equals("columnSpace")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        List<double[]> basis = MatrixMath.columnSpace(A);
                        Map<String, Object> resObj = new HashMap<>();
                        resObj.put("basis", basis);
                        responseMap.put("result", resObj);
                    } else if (mappedOperation.equals("rowSpace")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        List<double[]> basis = MatrixMath.rowSpace(A);
                        Map<String, Object> resObj = new HashMap<>();
                        resObj.put("basis", basis);
                        responseMap.put("result", resObj);
                    } else if (mappedOperation.equals("leftNullSpace")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        List<double[]> basis = MatrixMath.leftNullSpace(A);
                        Map<String, Object> resObj = new HashMap<>();
                        resObj.put("basis", basis);
                        responseMap.put("result", resObj);
                    } else if (mappedOperation.equals("solve") || mappedOperation.equals("solveLU")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[] b = getVectorFromParam(requestMap.get("vectorB"));
                        boolean showSteps = requestMap.containsKey("showRowOpSteps") && (Boolean) requestMap.get("showRowOpSteps");
                        if (showSteps) {
                            // Show steps using augmented matrix [A | b]
                            int m = A.length;
                            int n = A[0].length;
                            double[][] augmented = new double[m][n + 1];
                            for (int i = 0; i < m; i++) {
                                System.arraycopy(A[i], 0, augmented[i], 0, n);
                                augmented[i][n] = b[i];
                            }
                            Map<String, Object> stepsRes = MatrixMath.rrefWithSteps(augmented);
                            List<Map<String, Object>> stepsList = (List<Map<String, Object>>) stepsRes.get("steps");
                            List<String> stepsStrings = new ArrayList<>();
                            stepsStrings.add("Gaussian elimination on augmented matrix [A | b]:");
                            for (Map<String, Object> step : stepsList) {
                                String desc = (String) step.get("description");
                                double[][] matState = (double[][]) step.get("matrix");
                                stepsStrings.add(desc + ":\n" + formatMatrixToString(matState));
                            }
                            
                            double[] x = MatrixMath.solveLU(A, b);
                            List<Double> xList = new ArrayList<>();
                            for (double val : x) xList.add(val);
                            
                            Map<String, Object> resObj = new HashMap<>();
                            resObj.put("result", xList);
                            resObj.put("steps", stepsStrings);
                            responseMap.put("result", resObj);
                        } else {
                            double[] x = MatrixMath.solveLU(A, b);
                            List<Double> xList = new ArrayList<>();
                            for (double val : x) xList.add(val);
                            responseMap.put("result", xList);
                        }
                    } else if (mappedOperation.equals("solveCramer")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[] b = getVectorFromParam(requestMap.get("vectorB"));
                        double[] x = MatrixMath.solveCramer(A, b);
                        List<Double> xList = new ArrayList<>();
                        for (double val : x) xList.add(val);
                        responseMap.put("result", xList);
                    } else if (mappedOperation.equals("solveLeastSquares")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[] b = getVectorFromParam(requestMap.get("vectorB"));
                        double[] x = MatrixMath.solveLeastSquares(A, b);
                        List<Double> xList = new ArrayList<>();
                        for (double val : x) xList.add(val);
                        responseMap.put("result", xList);
                    } else if (mappedOperation.equals("decomposeLU")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        Map<String, double[][]> decomp = MatrixMath.decomposeLU(A);
                        Map<String, Object> resObj = new HashMap<>();
                        resObj.put("L", decomp.get("L"));
                        resObj.put("U", decomp.get("U"));
                        resObj.put("P", decomp.get("P"));
                        responseMap.put("result", resObj);
                    } else if (mappedOperation.equals("decomposeQR")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        Map<String, double[][]> decomp = MatrixMath.decomposeQR(A);
                        Map<String, Object> resObj = new HashMap<>();
                        resObj.put("Q", decomp.get("Q"));
                        resObj.put("R", decomp.get("R"));
                        responseMap.put("result", resObj);
                    } else if (mappedOperation.equals("decomposeCholesky")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[][] L = MatrixMath.decomposeCholesky(A);
                        Map<String, Object> resObj = new HashMap<>();
                        resObj.put("L", L);
                        responseMap.put("result", resObj);
                    } else if (mappedOperation.equals("characteristicPolynomial")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double[] coeffs = MatrixMath.characteristicPolynomial(A);
                        String polyStr = formatCharPoly(coeffs);
                        responseMap.put("result", polyStr);
                    } else if (mappedOperation.equals("eigenvalues")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        MatrixMath.Complex[] ev = MatrixMath.eigenvalues(A);
                        List<Object> list = new ArrayList<>();
                        for (MatrixMath.Complex val : ev) {
                            if (Math.abs(val.im) < 1e-9) {
                                list.add(val.re);
                            } else {
                                list.add(formatComplex(val));
                            }
                        }
                        responseMap.put("result", list);
                    } else if (mappedOperation.equals("eigenvectors")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double lambda = ((Number) requestMap.get("lambda")).doubleValue();
                        List<double[]> list = MatrixMath.eigenvectors(A, lambda);
                        responseMap.put("result", list);
                    } else if (mappedOperation.equals("eigen")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        MatrixMath.Complex[] ev = MatrixMath.eigenvalues(A);
                        List<Object> listEV = new ArrayList<>();
                        List<double[]> listVectors = new ArrayList<>();
                        
                        double[] charPolyCoeffs = MatrixMath.characteristicPolynomial(A);
                        String charPolyStr = formatCharPoly(charPolyCoeffs);
                        
                        for (MatrixMath.Complex val : ev) {
                            if (Math.abs(val.im) < 1e-9) {
                                listEV.add(val.re);
                            } else {
                                listEV.add(formatComplex(val));
                            }
                            
                            if (Math.abs(val.im) < 1e-9) {
                                List<double[]> vecs = MatrixMath.eigenvectors(A, val.re);
                                if (!vecs.isEmpty()) {
                                    listVectors.add(vecs.get(0));
                                } else {
                                    listVectors.add(new double[A.length]);
                                }
                            } else {
                                listVectors.add(new double[A.length]);
                            }
                        }
                        
                        Map<String, Object> resObj = new HashMap<>();
                        resObj.put("charPolynomial", charPolyStr);
                        resObj.put("eigenvalues", listEV);
                        resObj.put("eigenvectors", listVectors);
                        responseMap.put("result", resObj);
                    } else if (mappedOperation.equals("trace")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        double tr = MatrixMath.trace(A);
                        responseMap.put("result", tr);
                    } else if (mappedOperation.equals("properties")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        Map<String, Double> norms = MatrixMath.norms(A);
                        Map<String, Double> formattedNorms = new HashMap<>();
                        formattedNorms.put("frobenius_norm", norms.get("frobenius"));
                        formattedNorms.put("one_norm", norms.get("oneNorm"));
                        formattedNorms.put("infinity_norm", norms.get("infinityNorm"));
                        
                        Map<String, Object> resObj = new HashMap<>();
                        resObj.put("norms", formattedNorms);
                        responseMap.put("result", resObj);
                    } else if (mappedOperation.equals("classifyMatrix")) {
                        double[][] A = getMatrixFromParam(requestMap.get("matrixA"), sessionId);
                        List<String> classifications = MatrixMath.classifyMatrix(A);
                        Map<String, String> properties = new HashMap<>();
                        properties.put("trace", formatDouble(MatrixMath.trace(A)));
                        properties.put("matrix_types", String.join(", ", classifications));
                        
                        Map<String, Object> resObj = new HashMap<>();
                        resObj.put("properties", properties);
                        responseMap.put("result", resObj);
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

        private String formatDouble(double val) {
            if (Math.abs(val - Math.round(val)) < 1e-9) {
                return String.valueOf((long) Math.round(val));
            }
            return String.format("%.4f", val);
        }

        private String formatMatrixToString(double[][] M) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < M.length; i++) {
                sb.append("[ ");
                for (int j = 0; j < M[i].length; j++) {
                    sb.append(formatDouble(M[i][j]));
                    if (j < M[i].length - 1) sb.append(", ");
                }
                sb.append(" ]");
                if (i < M.length - 1) sb.append("\n");
            }
            return sb.toString();
        }

        private String formatCharPoly(double[] coeffs) {
            int n = coeffs.length - 1;
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (int i = n; i >= 0; i--) {
                double val = coeffs[i];
                if (Math.abs(val) < 1e-9) continue;
                
                if (val > 0) {
                    if (!first) sb.append(" + ");
                } else {
                    if (!first) sb.append(" - ");
                    else sb.append("-");
                }
                
                double absVal = Math.abs(val);
                if (absVal != 1.0 || i == 0) {
                    sb.append(formatDouble(absVal));
                }
                
                if (i > 0) {
                    sb.append("\u03bb");
                    if (i > 1) {
                        sb.append("^").append(i);
                    }
                }
                first = false;
            }
            return sb.toString();
        }

        private String formatComplex(MatrixMath.Complex c) {
            String reStr = formatDouble(c.re);
            String imStr = formatDouble(Math.abs(c.im));
            if (c.im >= 0) {
                return reStr + " + " + imStr + "i";
            } else {
                return reStr + " - " + imStr + "i";
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
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
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
}
