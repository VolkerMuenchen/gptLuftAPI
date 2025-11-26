package net.envinet.pm25;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.stream.Collectors;

public class SensorFinder {

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.out.println("Bitte Sensor-ID angeben. Beispiel:");
            System.out.println("  java SensorFinder 81607");
            return;
        }

        String baseId = args[0];

        System.out.println("Suche Koordinaten für Sensor " + baseId + " …");

        double[] baseCoords = getCoordinates(baseId);

        if (baseCoords == null) {
            System.out.println("Fehler: Sensor " + baseId + " hat keine Koordinaten.");
            return;
        }

        double baseLat = baseCoords[0];
        double baseLon = baseCoords[1];

        System.out.println("Basis-Sensor: lat=" + baseLat + " lon=" + baseLon);

        System.out.println("Lade alle Sensoren der Region (ca. 25 km Radius) …");

        List<SensorEntry> all = fetchAllNearby(baseLat, baseLon);

        if (all.isEmpty()) {
            System.out.println("Keine Sensoren gefunden.");
            return;
        }

        System.out.println("Sortiere nach Entfernung …");

        List<SensorEntry> nearest =
                all.stream()
                        .sorted(Comparator.comparingDouble(s -> distance(baseLat, baseLon, s.lat, s.lon)))
                        .limit(5)
                        .collect(Collectors.toList());

        System.out.println("\n=============================");
        System.out.println("JSON für sensors.json:");
        System.out.println("=============================\n");

System.out.println("[");
for (int i = 0; i < nearest.size(); i++) {
    SensorEntry s = nearest.get(i);
    System.out.printf(
            Locale.US,                                           // <<< hier neu
            "  { \"id\": \"%s\", \"lat\": %.6f, \"lon\": %.6f }%s%n",
            s.id, s.lat, s.lon,
            (i < nearest.size() - 1 ? "," : "")
    );
}
System.out.println("]");


        System.out.println("\nFertig!");
    }

    // --------------------------------------------------------------------
    // API-Datenmodel
    // --------------------------------------------------------------------

    static class SensorEntry {
        String id;
        double lat;
        double lon;
    }

    // --------------------------------------------------------------------
    // SensorCommunity API: Sensor-Koordinaten laden
    // --------------------------------------------------------------------

    private static double[] getCoordinates(String id) throws Exception {
        String url = "https://data.sensor.community/static/v2/data.json";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "gptLuftAPI/1.0 (Java)")
                .GET()
                .build();

        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode root = MAPPER.readTree(res.body());

        for (JsonNode node : root) {
            JsonNode sensor = node.path("sensor");
            if (!sensor.isMissingNode() && sensor.path("id").asText().equals(id)) {
                double lat = node.path("location").path("latitude").asDouble();
                double lon = node.path("location").path("longitude").asDouble();
                return new double[]{lat, lon};
            }
        }
        return null;
    }

    // --------------------------------------------------------------------
    // Alle Sensoren (in großem Umkreis) laden
    // --------------------------------------------------------------------

    private static List<SensorEntry> fetchAllNearby(double lat0, double lon0) throws Exception {
        String url = "https://data.sensor.community/static/v2/data.json";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "gptLuftAPI/1.0 (Java)")
                .GET()
                .build();

        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode root = MAPPER.readTree(res.body());

        List<SensorEntry> list = new ArrayList<>();

        for (JsonNode node : root) {
            JsonNode sensor = node.path("sensor");
            if (sensor.isMissingNode()) continue;

            String id = sensor.path("id").asText();
            double lat = node.path("location").path("latitude").asDouble();
            double lon = node.path("location").path("longitude").asDouble();

            // Ausschließen von sinnlosen Koordinaten
            if (lat == 0 || lon == 0) continue;

            // grober Umkreisfilter (~25 km)
            if (distance(lat0, lon0, lat, lon) < 25.0) {
                SensorEntry s = new SensorEntry();
                s.id = id;
                s.lat = lat;
                s.lon = lon;
                list.add(s);
            }
        }

        return list;
    }

    // --------------------------------------------------------------------
    // Haversine-Distanz (km)
    // --------------------------------------------------------------------

    private static double distance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}
