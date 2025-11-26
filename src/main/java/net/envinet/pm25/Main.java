package net.envinet.pm25;

public class Main {
    public static void main(String[] args) throws Exception {
        final long INTERVALL_MS = 30L * 60L * 1000L; // 30 Minuten

        while (true) {
            try {
                SensorUpdater.fetchAndStore();         // Daten aktualisieren
                KartenGenerator.exportHtmlWithChart(); // Karte (inkl. Charts) neu erzeugen
                System.out.println("➡️  Warte 30 Minuten …");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("⚠️  Fehler beim Aktualisieren – nächster Versuch später.");
            }
            Thread.sleep(INTERVALL_MS);
        }
    }
}
