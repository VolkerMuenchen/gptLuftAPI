package net.envinet.pm25;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class KartenGenerator {

    /**
     * Erzeugt eine HTML-Datei mit:
     *  - Leaflet-Karte
     *  - Marker für jeden Sensor
     *  - Farbe abhängig vom letzten PM2.5-Wert
     *  - Chart.js-Verlauf beim Klick
     */
    public static void exportHtmlWithChart() throws IOException {

        // ---------------------------------------------------------
        // 1. CSV-Daten aus data/sensor_<id>.csv laden
        // ---------------------------------------------------------
        Map<String, List<String[]>> daten = new LinkedHashMap<>();

        for (String id : SensorUpdater.getSensorIds()) {
            Path p = Path.of("data", "sensor_" + id + ".csv");

            if (Files.exists(p)) {
                List<String[]> rows = Files.readAllLines(p, StandardCharsets.UTF_8).stream()
                        .filter(l -> !l.isBlank())
                        .map(l -> l.split(",", -1))
                        .filter(a -> a.length >= 2)
                        .sorted(Comparator.comparing(a -> a[0]))
                        .collect(Collectors.toList());

                if (!rows.isEmpty())
                    daten.put(id, rows);
            }
        }

        // ---------------------------------------------------------
        // 2. Starte HTML-Datei
        // ---------------------------------------------------------
        StringBuilder html = new StringBuilder();
        html.append("""
                <!DOCTYPE html>
                <html lang='de'>
                <head>
                  <meta charset='utf-8'/>
                  <meta name='viewport' content='width=device-width, initial-scale=1'/>
                  <title>PM2.5 Karte</title>
                  <link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>
                  <script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>
                  <script src='https://cdn.jsdelivr.net/npm/chart.js'></script>
                  <style>
                    body{font-family:system-ui,Segoe UI,Roboto,Arial,sans-serif;margin:0;padding:0}
                    h2{margin:12px 16px}
                    #map{height:72vh;margin:0 16px;border-radius:8px}
                    .legend{position:absolute;bottom:16px;left:26px;background:#fff;padding:8px 10px;border-radius:6px;box-shadow:0 2px 8px rgba(0,0,0,.15);line-height:1.4}
                    .legend .dot{font-size:18px;vertical-align:middle;margin-right:6px}
                  </style>
                </head>
                <body>
                  <h2>PM2.5 Sensorwerte mit Mini-Charts (Klick auf Marker)</h2>
                  <div id='map'></div>

                  <div class='legend'><b>Legende</b><br>
                    <span class='dot' style='color:green'>●</span> ≤ 10&nbsp;&nbsp;
                    <span class='dot' style='color:orange'>●</span> 11–20&nbsp;&nbsp;
                    <span class='dot' style='color:red'>●</span> > 20
                  </div>

                  <script>
                    var map = L.map('map').setView([48.5216, 9.0576], 14);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {maxZoom: 19}).addTo(map);

                    var sensors = {};
                """);

        // ---------------------------------------------------------
        // 3. JavaScript-Datenobjekte aufbauen
        // ---------------------------------------------------------
        for (Map.Entry<String, List<String[]>> e : daten.entrySet()) {
            String id = e.getKey();
            List<String[]> rows = e.getValue();

            SensorConfig cfg = ConfigLoader.findById(id);
            double lat = cfg != null ? cfg.lat : 48.5216;
            double lon = cfg != null ? cfg.lon : 9.0576;

            String last = rows.get(rows.size() - 1)[1];
            double pm = parseDouble(last);
            String farbe = pm <= 10 ? "green" : (pm <= 20 ? "orange" : "red");

            String labels = rows.stream()
                    .map(r -> "'" + extractTime(r[0]) + "'")
                    .collect(Collectors.joining(","));

            String values = rows.stream()
                    .map(r -> r[1])
                    .collect(Collectors.joining(","));

            html.append(String.format(Locale.US,
                    "sensors['%s'] = {lat:%f, lon:%f, color:'%s', labels:[%s], values:[%s]};\n",
                    id, lat, lon, farbe, labels, values));
        }

        // ---------------------------------------------------------
        // 4. Marker + Chart.js Interaktion
        // ---------------------------------------------------------
        for (String id : daten.keySet()) {
            html.append(String.format(
                    """
                    (function(){
                        var d = sensors['%s'];
                        var m = L.circleMarker([d.lat, d.lon], {radius: 10, color: d.color}).addTo(map);

                        m.on('click', function(){
                            if (d.values.length >= 3) {
                                var cid = 'chart_%s';
                                var content =
                                  "<b>Sensor %s</b><br>Messpunkte: " + d.values.length +
                                  "<div style='width:320px;margin-top:6px'>" +
                                  "<canvas id='" + cid + "' width='320' height='160'></canvas>" +
                                  "</div>";
                                this.bindPopup(content).openPopup();

                                setTimeout(function(){
                                    var canvas = document.getElementById(cid);
                                    if (!canvas) return;
                                    var ctx = canvas.getContext('2d');

                                    new Chart(ctx, {
                                        type: 'line',
                                        data: {
                                            labels: d.labels,
                                            datasets: [{
                                                label: 'PM2.5',
                                                data: d.values,
                                                borderColor: 'blue',
                                                tension: 0.2
                                            }]
                                        },
                                        options: {
                                            responsive: false,
                                            scales: { y: { beginAtZero: true } }
                                        }
                                    });
                                }, 80);

                            } else {
                                this.bindPopup("<b>Sensor %s</b><br>Nicht genug Daten (mind. 3 Punkte).").openPopup();
                            }
                        });

                    })();
                    """,
                    id, id, id, id));
        }

        // ---------------------------------------------------------
        // 5. HTML abschließen
        // ---------------------------------------------------------
        html.append("""
                  </script>
                </body>
                </html>
                """);

        Files.writeString(Path.of("karte_mit_chart.html"), html.toString(), StandardCharsets.UTF_8);
        System.out.println("✔ Karte erzeugt: karte_mit_chart.html");
    }

    // --------------------------------------------------------------------
    // Hilfsfunktionen
    // --------------------------------------------------------------------
    private static double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception ex) { return Double.NaN; }
    }

    private static String extractTime(String timestamp) {
        if (timestamp == null || timestamp.length() < 16) return timestamp;
        return timestamp.substring(11, 16); // HH:mm
    }
}
