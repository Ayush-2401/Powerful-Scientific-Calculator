package com.scientificcalculator.engine;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class UnitConverterTest {

    @Test
    void testConvertTemperature_celsiusToFahrenheit() {
        BigDecimal result = UnitConverter.convertUnit("temperature", new BigDecimal("100"), "Celsius", "Fahrenheit");
        assertEquals(0, new BigDecimal("212").compareTo(result));
    }

    @Test
    void testConvertTemperature_fahrenheitToCelsius() {
        BigDecimal result = UnitConverter.convertUnit("temperature", new BigDecimal("32"), "Fahrenheit", "Celsius");
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void testConvertTemperature_celsiusToKelvin() {
        BigDecimal result = UnitConverter.convertUnit("temperature", new BigDecimal("0"), "Celsius", "Kelvin");
        assertEquals(0, new BigDecimal("273.15").compareTo(result));
    }

    @Test
    void testConvertPressure_atmToPa() {
        BigDecimal result = UnitConverter.convertUnit("pressure", new BigDecimal("1"), "atm", "pa");
        assertEquals(0, new BigDecimal("101325").compareTo(result));
    }

    @Test
    void testConvertPressure_barToPa() {
        BigDecimal result = UnitConverter.convertUnit("pressure", new BigDecimal("1.5"), "bar", "pa");
        assertEquals(0, new BigDecimal("150000").compareTo(result));
    }

    @Test
    void testConvertEnergy_whToJ() {
        BigDecimal result = UnitConverter.convertUnit("energy", new BigDecimal("2"), "wh", "j");
        assertEquals(0, new BigDecimal("7200").compareTo(result));
    }

    @Test
    void testConvertBase_hexToBin() {
        String result = UnitConverter.convertBase("FF", "hex", "bin");
        assertEquals("11111111", result);
    }

    @Test
    void testConvertBase_decToHex() {
        String result = UnitConverter.convertBase("255", "10", "16");
        assertEquals("FF", result);
    }

    @Test
    void testInvalidUnit_throws() {
        assertThrows(IllegalArgumentException.class, () -> {
            UnitConverter.convertUnit("pressure", BigDecimal.ONE, "unknown", "pa");
        });
    }
}
