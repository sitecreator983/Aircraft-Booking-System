package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/airports")
public class AirportController {

    public record AirportOption(String name, String code, String display) {}

    // ✅ Εδώ βάζεις τη “συγκεκριμένη λίστα” σου
    private static final List<AirportOption> AIRPORTS = List.of(
            opt("THESSALONIKI", "SKG"),
            opt("SAMOS", "SMI"),
            opt("ATHENS", "ATH"),
            opt("HERAKLION", "HER"),
            opt("RHODES", "RHO"),
            opt("CHANIA", "CHQ"),
            opt("ALEXANDROUPOLI", "AXD"),
            opt("CHIOS", "JKH"),
            opt("CORFU", "CFU"),
            opt("HERAKLION", "HER"),
            opt("MUNICH", "MUC"),
            opt("DUSSELDORF", "DUS"),
            opt("FRANKFURT", "FRA"),
            opt("HANNOVER", "HAJ"),
            opt("HAMBURG", "HAM"),
            opt("BERLIN", "BER"),
            opt("ROME", "FCO"),
            opt("MILAN", "MXP"),
            opt("NAPLES", "NAP"),
            opt("PARIS", "CDG"),
            opt("LYON", "LYS"),
            opt("MADRID", "MAD"),
            opt("BARCELONA", "BCN"),
            opt("DUBAI", "DXB"),
            opt("ABU DHABI", "AUH")
            // πρόσθεσε όσα θέλεις
    );

    private static AirportOption opt(String name, String code) {
        return new AirportOption(name, code, name + "(" + code + ")");
    }

    @GetMapping
    public List<AirportOption> all() {
        return AIRPORTS;
    }

    // helper για validation από άλλους controllers (αν το χρειαστείς)
    public static boolean isValidCode(String code) {
        if (code == null) return false;
        String c = code.trim().toUpperCase();
        return AIRPORTS.stream().anyMatch(a -> a.code().equalsIgnoreCase(c));
    }
}
