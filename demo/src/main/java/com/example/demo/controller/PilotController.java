package com.example.demo.controller;

import com.example.demo.model.Pilot;
import com.example.demo.repository.PilotRepository;
import org.springframework.web.bind.annotation.*;
import com.example.demo.model.Airline;
import com.example.demo.repository.FlightRepository;

import java.util.List;

@RestController
@RequestMapping("/api/pilots")
public class PilotController {

    private final PilotRepository repo;

    private final FlightRepository flightRepo;

    public PilotController(PilotRepository repo, FlightRepository flightRepo) {
        this.repo = repo;
        this.flightRepo = flightRepo;
    }

    public record PilotRequest(
            String firstName,
            String lastName,
            String dateOfBirth,   // YYYY-MM-DD
            String phone,
            String email,
            String licenseNumber,
            String nationality,
            String hireDate  ,     // YYYY-MM-DD
            String airlineCode  // e.g. "AE", "KL", ...
    ) {}

    public record PilotResponse(
            Integer pilotID,
            String firstName,
            String lastName,
            String dateOfBirth,
            String phone,
            String email,
            String licenseNumber,
            String nationality,
            String hireDate,
            String airlineName,
            String airlineCode
    ) {}

    public record AvailabilityResponse(Integer pilotID, boolean available, long conflicts) {}


    @GetMapping
    public List<PilotResponse> all() {
        return repo.findAll().stream().map(this::toRes).toList();
    }

    @GetMapping("/{id}")
    public PilotResponse one(@PathVariable Integer id) {
        return toRes(repo.findById(id).orElseThrow(() -> new NotFoundException("Pilot not found: " + id)));
    }

    @GetMapping("/by-airline/{code}")
    public List<PilotResponse> byAirline(@PathVariable String code) {
        Airline airline = Airline.fromCode(code);
        return repo.findByAirline(airline).stream().map(this::toRes).toList();
    }

    @GetMapping("/{id}/availability")
    public AvailabilityResponse availability(
        @PathVariable Integer id,
        @RequestParam String departureTime,
        @RequestParam String arrivalTime,
        @RequestParam(required = false) String excludeFlightNumber
    ) {
        repo.findById(id).orElseThrow(() -> new NotFoundException("Pilot not found: " + id));

        var dep = java.time.LocalDateTime.parse(departureTime);
        var arr = java.time.LocalDateTime.parse(arrivalTime);
        if (!arr.isAfter(dep)) throw new ApiException("arrivalTime must be after departureTime");

        var depMinus3 = dep.minusHours(3);
        var busyEnd = arr.plusHours(3);

        long conflicts = (excludeFlightNumber == null || excludeFlightNumber.isBlank())
            ? flightRepo.countPilotConflicts(id, depMinus3, busyEnd)
            : flightRepo.countPilotConflictsExcludingFlight(id, depMinus3, busyEnd, excludeFlightNumber.trim());

        return new AvailabilityResponse(id, conflicts == 0, conflicts);
    }

    @PostMapping
    public PilotResponse create(@RequestBody PilotRequest req) {
        if (req.licenseNumber() == null || req.licenseNumber().isBlank()) {
            throw new ApiException("licenseNumber is required");
        }
        if (req.airlineCode() == null || req.airlineCode().isBlank()) {
            throw new ApiException("airlineCode is required");
        }
        Pilot p = new Pilot();
        apply(p, req);
        return toRes(repo.save(p));
    }

    @PutMapping("/{id}")
    public PilotResponse update(@PathVariable Integer id, @RequestBody PilotRequest req) {
        Pilot p = repo.findById(id).orElseThrow(() -> new NotFoundException("Pilot not found: " + id));
        apply(p, req);
        return toRes(repo.save(p));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        if (!repo.existsById(id)) throw new NotFoundException("Pilot not found: " + id);
        repo.deleteById(id);
    }

    private PilotResponse toRes(Pilot p) {
        return new PilotResponse(
                p.getPilotID(),
                p.getFirstName(),
                p.getLastName(),
                p.getDateOfBirth() == null ? null : p.getDateOfBirth().toString(),
                p.getPhone(),
                p.getEmail(),
                p.getLicenseNumber(),
                p.getNationality(),
                p.getHireDate() == null ? null : p.getHireDate().toString(),
                p.getAirline() == null ? null : p.getAirline().getDisplayName(),
                p.getAirline() == null ? null : p.getAirline().getCode()
        );
    }

    private void apply(Pilot p, PilotRequest req) {
        if (req.firstName() != null) p.setFirstName(req.firstName());
        if (req.lastName() != null) p.setLastName(req.lastName());
        if (req.phone() != null) p.setPhone(req.phone());
        if (req.email() != null) p.setEmail(req.email());
        if (req.licenseNumber() != null) p.setLicenseNumber(req.licenseNumber());
        if (req.nationality() != null) p.setNationality(req.nationality());

        if (req.dateOfBirth() != null && !req.dateOfBirth().isBlank())
            p.setDateOfBirth(java.time.LocalDate.parse(req.dateOfBirth()));

        if (req.hireDate() != null && !req.hireDate().isBlank())
            p.setHireDate(java.time.LocalDate.parse(req.hireDate()));

        if (req.airlineCode() != null && !req.airlineCode().isBlank()) {
            p.setAirline(Airline.fromCode(req.airlineCode()));
        }
    }
}