package net.envinet.pm25;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensorFinderTest {

    @Test
    void haversine_zeroDistanceForSamePoint() {
        double km = SensorFinder.haversineKm(48.5216, 9.0576, 48.5216, 9.0576);
        assertEquals(0.0, km, 1e-9);
    }

    @Test
    void haversine_about111kmPerDegreeLatitude() {
        // 1 Grad Latitude ~ 111 km
        double km = SensorFinder.haversineKm(0.0, 0.0, 1.0, 0.0);
        assertTrue(km > 110 && km < 112, "Erwartet ~111 km, war: " + km);
    }

    @Test
    void jsonLine_usesDotDecimalSeparator() {
        String line = SensorFinder.formatSensorJsonLine("81607", 48.5216, 9.0576, true);
        assertTrue(line.contains("48.521600"), "lat sollte Dezimalpunkt haben: " + line);
        assertTrue(line.contains("9.057600"), "lon sollte Dezimalpunkt haben: " + line);
        assertFalse(line.contains("48,521600"), "lat darf kein Komma haben: " + line);
        assertFalse(line.contains("9,057600"), "lon darf kein Komma haben: " + line);
        assertTrue(line.trim().endsWith(","), "Zeile sollte mit Komma enden (trailingComma=true): " + line);
    }
}
