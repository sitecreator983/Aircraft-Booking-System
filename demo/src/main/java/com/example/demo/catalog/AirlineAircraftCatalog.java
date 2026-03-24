package com.example.demo.catalog;

import com.example.demo.model.Airline;

import java.util.*;

public class AirlineAircraftCatalog {

    // Airline -> (Model -> Count)
    private static final Map<Airline, Map<String, Integer>> FLEET;

    static {
        Map<Airline, Map<String, Integer>> map = new EnumMap<>(Airline.class);

        // ΒΑΛΕ ΕΔΩ ΤΟΥΣ ΑΡΙΘΜΟΥΣ ΠΟΥ ΘΕΣ ΕΣΥ (counts)
        map.put(Airline.AEGEAN, fleet(
                entry("Airbus A320neo", 3),
                entry("Airbus A321neo", 2),
                entry("Airbus A320", 2),
                entry("Airbus A321", 1)
        ));

        map.put(Airline.SKYEXPRESS, fleet(
                entry("Airbus A321neo", 2),
                entry("Airbus A320neo", 2),
                entry("Airbus A320ceo", 1),
                entry("ATR 72-600", 2),
                entry("ATR 42-500", 1)
        ));

        map.put(Airline.OLYMPICAIRWAYS, fleet(
                entry("ATR 72-600", 2),
                entry("ATR 42-500", 1),
                entry("Dash 8-100", 1)
        ));

        map.put(Airline.KLM, fleet(
                entry("Boeing 787", 2),
                entry("Boeing 777-200ER", 1),
                entry("Boeing 737-700", 2),
                entry("Airbus A330-220", 1)
        ));

        map.put(Airline.LUFTHANSA, fleet(
                entry("Airbus A380", 1),
                entry("Airbus A350-900", 2),
                entry("Airbus A340-300", 1),
                entry("Airbus A330-300", 1),
                entry("Airbus A321neo", 2),
                entry("Airbus A320neo", 2),
                entry("Airbus A319", 1),
                entry("Boeing 747-400", 1),
                entry("Boeing 787", 2)
        ));

        map.put(Airline.AIRFRANCE, fleet(
                entry("Airbus A350-900", 2),
                entry("Airbus A330-200", 1),
                entry("Airbus A320", 2),
                entry("Airbus A321", 1),
                entry("Airbus A220-300", 1),
                entry("Boeing 777-200ER", 1),
                entry("Boeing 787-9", 1)
        ));

        map.put(Airline.BRITISHAIRWAYS, fleet(
                entry("Airbus A380", 1),
                entry("Airbus A350-1000", 1),
                entry("Airbus A320neo", 2),
                entry("Airbus A321neo", 1),
                entry("Boeing 777-200", 1),
                entry("Boeing 787", 2)
        ));

        map.put(Airline.SAS, fleet(
                entry("Airbus A350-900", 1),
                entry("Airbus A330-300", 1),
                entry("Airbus A320neo", 2),
                entry("Airbus A321LR", 1),
                entry("Embraer E195", 1)
        ));

        map.put(Airline.TURKISHAIRLINES, fleet(
                entry("Boeing 787-9", 1),
                entry("Boeing 777-300ER", 1),
                entry("Boeing 737-800/900/MAX", 2),
                entry("Airbus A350-900", 1),
                entry("Airbus A330-200", 1),
                entry("Airbus A321neo", 1),
                entry("Airbus A320", 1)
        ));

        map.put(Airline.FLYEMIRATES, fleet(
                entry("Airbus A380-800", 1),
                entry("Boeing 777-200LR", 1)
        ));

        map.put(Airline.QATARAIRWAYS, fleet(
                entry("Airbus A380", 1),
                entry("Airbus A350-900", 1),
                entry("Airbus A320", 1),
                entry("Boeing 777-200LR", 1),
                entry("Boeing 787", 1)
        ));

        map.put(Airline.QANTAS, fleet(
                entry("Boeing 787-9", 1),
                entry("Boeing 737-800", 1),
                entry("Airbus A380", 1),
                entry("Airbus A330-200", 1),
                entry("Airbus A220-300", 1),
                entry("Airbus A321XLR", 1)
        ));

        map.put(Airline.EASYJET, fleet(
                entry("Airbus A319", 1),
                entry("Airbus A320", 2),
                entry("Airbus A320neo", 1),
                entry("Airbus A321neo", 1)
        ));

        map.put(Airline.WIZZAIR, fleet(
                entry("Airbus A320ceo", 1),
                entry("Airbus A320neo", 1),
                entry("Airbus A321ceo", 1),
                entry("Airbus A321neo", 1),
                entry("Airbus A321XLR", 1)
        ));

        map.put(Airline.AIRCANADA, fleet(
                entry("Boeing 777-200LR", 1),
                entry("Boeing 787", 1),
                entry("Boeing 737", 1),
                entry("Airbus A330-300", 1),
                entry("Airbus A321", 1),
                entry("Airbus A220-300", 1)
        ));

        map.put(Airline.EGYPTAIR, fleet(
                entry("Boeing 787-9", 1),
                entry("Boeing 777-300ER", 1),
                entry("Boeing 737-800", 1),
                entry("Airbus A350-900", 1),
                entry("Airbus A320neo", 1),
                entry("Airbus A321neo", 1)
        ));

        FLEET = Collections.unmodifiableMap(map);
    }

    public static List<String> modelsFor(Airline airline) {
        Map<String, Integer> m = FLEET.get(airline);
        if (m == null) return Collections.emptyList();
        return new ArrayList<>(m.keySet());
    }

    public static int fleetCountFor(Airline airline, String model) {
        Map<String, Integer> m = FLEET.get(airline);
        if (m == null) return 0;
        Integer c = m.get(model);
        return c == null ? 0 : c;
    }

    public static Map<String, Integer> fleetFor(Airline airline) {
        Map<String, Integer> m = FLEET.get(airline);
        return m == null ? Collections.emptyMap() : m;
    }

    // helpers (Java 8)
    private static Map.Entry<String, Integer> entry(String model, int count) {
        return new AbstractMap.SimpleEntry<>(model, count);
    }

    @SafeVarargs
    private static Map<String, Integer> fleet(Map.Entry<String, Integer>... entries) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : entries) m.put(e.getKey(), e.getValue());
        return Collections.unmodifiableMap(m);
    }
}