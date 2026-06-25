package com.scientificcalculator.engine;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class UnitConverter {

    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_UP);

    private static final Map<String, BigDecimal> PRESSURE_FACTORS = new HashMap<>();
    static {
        PRESSURE_FACTORS.put("pa", BigDecimal.ONE);
        PRESSURE_FACTORS.put("atm", new BigDecimal("101325"));
        PRESSURE_FACTORS.put("bar", new BigDecimal("100000"));
        PRESSURE_FACTORS.put("psi", new BigDecimal("6894.757293168"));
        PRESSURE_FACTORS.put("torr", new BigDecimal("133.322368421"));
    }

    private static final Map<String, BigDecimal> ENERGY_FACTORS = new HashMap<>();
    static {
        ENERGY_FACTORS.put("j", BigDecimal.ONE);
        ENERGY_FACTORS.put("cal", new BigDecimal("4.184"));
        ENERGY_FACTORS.put("kcal", new BigDecimal("4184"));
        ENERGY_FACTORS.put("wh", new BigDecimal("3600"));
        ENERGY_FACTORS.put("kwh", new BigDecimal("3600000"));
        ENERGY_FACTORS.put("ev", new BigDecimal("1.602176634E-19"));
    }

    // 1. High-precision Unit Conversion
    public static BigDecimal convertUnit(String category, BigDecimal value, String from, String to) {
        if (category == null || value == null || from == null || to == null) {
            throw new IllegalArgumentException("Parameters cannot be null.");
        }
        category = category.trim().toLowerCase();
        from = from.trim().toLowerCase();
        to = to.trim().toLowerCase();

        if (category.equals("temperature")) {
            return convertTemperature(value, from, to);
        }

        Map<String, BigDecimal> factors;
        if (category.equals("pressure")) {
            factors = PRESSURE_FACTORS;
        } else if (category.equals("energy")) {
            factors = ENERGY_FACTORS;
        } else {
            throw new IllegalArgumentException("Unknown conversion category: " + category);
        }

        BigDecimal fromFactor = factors.get(from);
        BigDecimal toFactor = factors.get(to);
        if (fromFactor == null || toFactor == null) {
            throw new IllegalArgumentException("Unsupported unit in " + category + ": '" + from + "' or '" + to + "'");
        }

        // Value in base unit (Pa or J)
        BigDecimal baseValue = value.multiply(fromFactor);
        // Value in target unit
        return baseValue.divide(toFactor, MC);
    }

    private static BigDecimal convertTemperature(BigDecimal value, String from, String to) {
        BigDecimal celsiusValue;

        // Convert to Celsius
        switch (from) {
            case "celsius": case "c":
                celsiusValue = value;
                break;
            case "fahrenheit": case "f":
                celsiusValue = value.subtract(new BigDecimal("32"))
                        .multiply(new BigDecimal("5"))
                        .divide(new BigDecimal("9"), MC);
                break;
            case "kelvin": case "k":
                celsiusValue = value.subtract(new BigDecimal("273.15"));
                break;
            default:
                throw new IllegalArgumentException("Unknown temperature unit: " + from);
        }

        // Convert from Celsius to target
        switch (to) {
            case "celsius": case "c":
                return celsiusValue;
            case "fahrenheit": case "f":
                return celsiusValue.multiply(new BigDecimal("1.8")).add(new BigDecimal("32"));
            case "kelvin": case "k":
                return celsiusValue.add(new BigDecimal("273.15"));
            default:
                throw new IllegalArgumentException("Unknown temperature unit: " + to);
        }
    }

    // 2. High-precision Base Conversion (Arbitrary Precision Integers)
    public static String convertBase(String value, String fromBaseStr, String toBaseStr) {
        if (value == null || fromBaseStr == null || toBaseStr == null) {
            throw new IllegalArgumentException("Parameters cannot be null.");
        }
        int fromRadix = parseBase(fromBaseStr);
        int toRadix = parseBase(toBaseStr);
        BigInteger number = new BigInteger(value.trim(), fromRadix);
        return number.toString(toRadix).toUpperCase();
    }

    private static int parseBase(String baseStr) {
        baseStr = baseStr.trim().toLowerCase();
        switch (baseStr) {
            case "binary": case "bin": case "2": return 2;
            case "octal": case "oct": case "8": return 8;
            case "decimal": case "dec": case "10": return 10;
            case "hexadecimal": case "hex": case "16": return 16;
            default: throw new IllegalArgumentException("Unsupported number base: " + baseStr);
        }
    }
}
