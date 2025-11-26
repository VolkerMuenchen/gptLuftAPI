package net.envinet.pm25;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class SensorUpdater {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .version(HttpClient.Version.HTTP_2)
            .build();

    private static final int MAX_RESULTS = 100;

    /** Liste der Sensor-IDs aus config/sensors.json */
    public static List<String> getSensorIds() {
        return ConfigLoader.getSensors().stream()
                .map(s -> s.id)
                .collect(Collectors.toList());
    }

    /** Gesamtprozess: Abrufen & Speichern */
    public static void fetchAndStore() throws IOException {

        Files.createDirectories(Path.of("data"));

        for (String id : getSensorIds()) {

            try {
                System.out.println("⇒ Sensor " + id + " abrufen …");

                List<String[]> fresh = fetchWithRetry(id, 3);

                int added = appendToCsv(id, fresh);

                System.out.println("   " + id + ": +" + added + " neue Zeilen");

            } catch (Exception e) {
                System.out.println("   Fehler bei Sensor " + id + ": " + e.getMessage());
            }

            try { Thread.sleep(ThreadLocalRandom.current().nextInt(150, 400)); }
            catch (InterruptedException ignored) {}
        }
    }

    private static List<String[]> fetchWithRetry(String id, int tries) {
        for (int i = 1; i <= tries; i++) {
            try { return fetchSensor(id); }
            catch (Exception e) {
                System.out.println("   Versuch " + i + " fehlgeschlagen: " + e.getMessage());
            }
        }
        return List.of();
    }

    private static List<String[]> fetchSensor(String id) throws IOException, InterruptedException {

        String url = "https://data.sensor.community/airrohr/v1/sensor/" +
                id + "/?type=pm&max_results=" + MAX_RESULTS;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "gptLuftAPI/1.0 (Java)")
                .GET()
                .build();

        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (res.statusCode() != 200) {
            throw new IOException("HTTP Status " + res.statusCode());
        }

        SensorApiEntry[] arr = MAPPER.readValue(res.body(), SensorApiEntry[].class);

        List<String[]> list = new ArrayList<>();

        for (SensorApiEntry e : arr) {
            if (e.timestamp == null || e.sensordatavalues == null) continue;

            String p2 = e.sensordatavalues.stream()
                    .filter(v -> "P2".equals(v.value_type))
                    .map(v -> v.value)
                    .findFirst()
                    .orElse(null);

            if (p2 != null)
                list.add(new String[]{e.timestamp, p2});
        }

        return list;
    }

    private static int appendToCsv(String id, List<String[]> rows) throws IOException {
        Path csv = Path.of("data", "sensor_" + id + ".csv");

        Set<String> known = new HashSet<>();
        if (Files.exists(csv)) {
            for (String line : Files.readAllLines(csv, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) known.add(line.split(",", -1)[0]);
            }
        }

        List<String[]> toAppend = rows.stream()
                .filter(r -> !known.contains(r[0]))
                .sorted(Comparator.comparing(r -> r[0]))
                .collect(Collectors.toList());

        if (toAppend.isEmpty()) return 0;

        try (BufferedWriter w = Files.newBufferedWriter(csv,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {

            for (String[] r : toAppend) {
                w.write(r[0] + "," + r[1]);
                w.newLine();
            }
        }

        return toAppend.size();
    }
}
