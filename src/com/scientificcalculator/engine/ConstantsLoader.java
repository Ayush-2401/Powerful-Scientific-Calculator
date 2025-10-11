// File: src/com/scientificcalculator/engine/ConstantsLoader.java

package com.scientificcalculator.engine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

// Jackson Imports
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;


public class ConstantsLoader {

    private static final String CACHE_DIR = System.getProperty("user.home") + "/.scientific_calculator";
    private static final String CACHED_CONSTANTS_FILE = CACHE_DIR + "/cached_constants.json";
    private static Map<String, Double> constants = new HashMap<>();
    private static Map<String, Map<String, Double>> categorizedConstants = new HashMap<>();
    private static boolean constantsLoaded = false;
    private static final ObjectMapper objectMapper = new ObjectMapper(); // Jackson ObjectMapper

    public static void loadConstants() {
        if (constantsLoaded) {
            return;
        }

        System.out.println("ConstantsLoader: Initiating constants loading process...");
        Path cacheDirPath = Paths.get(CACHE_DIR);

        if (!Files.exists(cacheDirPath)) {
            try {
                Files.createDirectories(cacheDirPath);
                System.out.println("ConstantsLoader: Created cache directory: " + CACHE_DIR);
            } catch (IOException e) {
                System.err.println("ConstantsLoader Error: Could not create cache directory: " + e.getMessage());
                // Continue and try to load from defaults anyway
            }
        }

        boolean loadedFromCacheSuccessfully = loadFromCache();

        if (loadedFromCacheSuccessfully) {
            System.out.println("ConstantsLoader: Constants loaded successfully from local cache.");
            constantsLoaded = true;
        } else {
            System.out.println("ConstantsLoader: Cache load failed or cache was empty. Loading default constants.");
            if (loadDefaultConstants()) {
                saveToCache();
                constantsLoaded = true;
            } else {
                throw new RuntimeException("CRITICAL: Failed to load constants from cache AND default resource. The application cannot start in a valid state. Ensure 'resources/constants.json' is on the classpath.");
            }
        }
    }

    private static boolean loadDefaultConstants() {
        constants.clear();
        categorizedConstants.clear();
        try (InputStream is = ConstantsLoader.class.getResourceAsStream("/resources/constants.json")) {
            if (is == null) {
                System.err.println("ConstantsLoader Error: Could not find default constants.json in resources.");
                System.err.println("Ensure the 'resources' directory is on the classpath.");
                return false;
            }
            JsonNode rootNode = objectMapper.readTree(is);

            rootNode.fields().forEachRemaining(categoryEntry -> {
                String categoryName = categoryEntry.getKey();
                JsonNode constantsArray = categoryEntry.getValue();
                if (constantsArray.isArray()) {
                    String capitalizedCategoryName = categoryName.substring(0, 1).toUpperCase() + categoryName.substring(1);
                    Map<String, Double> categoryMap = new HashMap<>();
                    for (JsonNode node : constantsArray) {
                        if (node.has("name") && node.has("value")) {
                            String name = node.get("name").asText().toUpperCase();
                            double value = node.get("value").asDouble();
                            constants.put(name, value);
                            categoryMap.put(name, value);
                        }
                    }
                    categorizedConstants.put(capitalizedCategoryName, categoryMap);
                }
            });
            System.out.println("ConstantsLoader: Successfully parsed and stored " + constants.size() + " constants from default resource.");
            return true;

        } catch (IOException e) {
            System.err.println("ConstantsLoader Error: Failed to load or parse default constants.json resource: " + e.getMessage());
            return false;
        }
    }

    private static boolean loadFromCache() {
        File cacheFile = new File(CACHED_CONSTANTS_FILE);
        if (!cacheFile.exists()) {
            System.out.println("ConstantsLoader: Cache file does not exist at " + CACHED_CONSTANTS_FILE + ".");
            return false;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(cacheFile);
            if (rootNode.isObject()) {
                constants.clear();
                categorizedConstants.clear();

                rootNode.fields().forEachRemaining(categoryEntry -> {
                    String categoryName = categoryEntry.getKey();
                    JsonNode categoryNode = categoryEntry.getValue();
                    if (categoryNode.isObject()) {
                        Map<String, Double> categoryMap = new HashMap<>();
                        categoryNode.fields().forEachRemaining(constantEntry -> {
                            String constantName = constantEntry.getKey();
                            JsonNode valueNode = constantEntry.getValue();
                            double value = 0;
                            boolean parsed = false;
                            if (valueNode.isNumber()) {
                                value = valueNode.asDouble();
                                parsed = true;
                            } else if (valueNode.isTextual()) {
                                try {
                                    value = Double.parseDouble(valueNode.asText());
                                    parsed = true;
                                } catch (NumberFormatException e) {
                                    System.err.println("ConstantsLoader Warning: Could not parse constant value as double for '" + constantName + "': " + valueNode.asText());
                                }
                            }
                            if (parsed) {
                                categoryMap.put(constantName, value);
                                constants.put(constantName, value);
                            }
                        });
                        categorizedConstants.put(categoryName, categoryMap);
                    }
                });
                System.out.println("ConstantsLoader: Cache loaded from " + CACHED_CONSTANTS_FILE);
                System.out.println("ConstantsLoader Debug: Constants loaded from cache: " + constants.size() + " constants.");
                if (constants.isEmpty()) {
                    System.out.println("ConstantsLoader: Cache file was valid but empty. Will reload from defaults.");
                    return false; // Treat empty cache as a failure to force reload from default
                }
                return true;
            } else {
                System.err.println("ConstantsLoader Error: Cached file is not a JSON object: " + CACHED_CONSTANTS_FILE);
                return false;
            }
        } catch (MismatchedInputException e) {
            System.err.println("ConstantsLoader Error: Cached file is empty or malformed JSON (MismatchedInputException): " + e.getMessage());
            return false;
        } catch (JsonParseException e) {
            System.err.println("ConstantsLoader Error: Cached file has invalid JSON syntax (JsonParseException): " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.err.println("ConstantsLoader Error: Could not load constants from cache: " + e.getMessage());
            return false;
        }
    }

    private static void saveToCache() {
        ObjectNode rootNode = objectMapper.createObjectNode();
        categorizedConstants.forEach((categoryName, constantsMap) -> {
            ObjectNode categoryNode = objectMapper.createObjectNode();
            constantsMap.forEach(categoryNode::put);
            rootNode.set(categoryName, categoryNode);
        });

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(CACHED_CONSTANTS_FILE), categorizedConstants);
            System.out.println("ConstantsLoader: Constants saved to cache: " + CACHED_CONSTANTS_FILE);
        } catch (IOException e) {
            System.err.println("ConstantsLoader Error: Could not save constants to cache: " + e.getMessage());
        }
    }

    public static Map<String, Double> getAllConstants() {
        if (!constantsLoaded) {
            loadConstants();
        }
        return constants;
    }

    public static Map<String, Map<String, Double>> getCategorizedConstants() {
        if (!constantsLoaded) {
            loadConstants();
        }
        return categorizedConstants;
    }

    public static double getConstantValue(String constantName) {
        if (!constantsLoaded) {
            loadConstants();
        }
        Double value = constants.get(constantName.toUpperCase());
        System.out.println("ConstantsLoader Debug: Attempting to get constant '" + constantName.toUpperCase() + "'. Value found: " + value);
        if (value == null) {
            throw new IllegalArgumentException("Unknown constant: " + constantName);
        }
        return value;
    }
}