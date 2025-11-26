package net.envinet.pm25;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class ConfigLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static List<SensorConfig> SENSORS;

    public static List<SensorConfig> getSensors() {
        if (SENSORS == null) {
            try {
                SENSORS = MAPPER.readValue(
                        Path.of("config", "sensors.json").toFile(),
                        new TypeReference<List<SensorConfig>>() {}
                );
            } catch (IOException e) {
                System.err.println("ConfigLoader: sensors.json konnte nicht gelesen werden: " + e.getMessage());
                SENSORS = Collections.emptyList();
            }
        }
        return SENSORS;
    }

    public static SensorConfig findById(String id) {
        return getSensors().stream()
                .filter(s -> s.id.equals(id))
                .findFirst()
                .orElse(null);
    }
}
