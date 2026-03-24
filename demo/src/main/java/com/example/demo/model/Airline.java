package com.example.demo.model;

import java.util.Arrays;

public enum Airline {
    KLM("KLM", "KL"),
    AEGEAN("Aegean", "AE"),
    LUFTHANSA("Lufthansa", "LH"),
    EASYJET("EasyJet", "EJ"),
    SKYEXPRESS("SkyExpress", "SE"),
    AIRFRANCE("AirFrance", "AF"),
    BRITISHAIRWAYS("BritishAirways", "BA"),
    OLYMPICAIRWAYS("OlympicAirways", "OL"),
    FLYEMIRATES("FlyEmirates", "EM"),
    QATARAIRWAYS("QatarAirways", "QA"),
    SAS("SAS", "SA"),
    TURKISHAIRLINES("TurkishAirlines", "TR"),
    QANTAS("Qantas", "QN"),
    WIZZAIR("WizzAir", "WZ"),
    EGYPTAIR("EgyptAir", "EG"),
    AIRCANADA("AirCanada", "AC");


    private final String displayName;
    private final String code;

    Airline(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName() { return displayName; }
    public String getCode() { return code; }

    public static Airline fromDisplayName(String name) {
        return Arrays.stream(values())
                .filter(a -> a.displayName.equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown airline: " + name));
    }

    public static Airline fromCode(String code) {
        return Arrays.stream(values())
            .filter(a -> a.code.equalsIgnoreCase(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown airline code: " + code));
    }
}
