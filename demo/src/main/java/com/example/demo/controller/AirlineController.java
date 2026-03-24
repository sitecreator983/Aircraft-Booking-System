package com.example.demo.controller;
import com.example.demo.catalog.AirlineAircraftCatalog;

import com.example.demo.model.Airline;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/airlines")
public class AirlineController {

    record AirlineDto(String name, String code) {}

    @GetMapping
    public List<AirlineDto> all() {
        return Arrays.stream(Airline.values())
                .map(a -> new AirlineDto(a.getDisplayName(), a.getCode()))
                .toList();
    }
    
    @GetMapping("/{code}/models")
    public List<String> modelsByAirline(@PathVariable String code) {
        Airline airline = Airline.fromCode(code);
        return AirlineAircraftCatalog.modelsFor(airline);
    }
}
