package com.example.demo.controller;

import com.example.demo.model.Aircraft;
import com.example.demo.model.Airline;
import com.example.demo.model.Pilot;
import com.example.demo.repository.AircraftRepository;
import com.example.demo.repository.FlightRepository;
import com.example.demo.repository.PilotRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/aircraft")
public class AircraftController {

    private final AircraftRepository aircraftRepo;
    private final PilotRepository pilotRepo;
    private final FlightRepository flightRepo;

    public AircraftController(AircraftRepository aircraftRepo, PilotRepository pilotRepo, FlightRepository flightRepo) {
        this.aircraftRepo = aircraftRepo;
        this.pilotRepo = pilotRepo;
        this.flightRepo = flightRepo;
    }

    // ===================== DTOs =====================

    public record AircraftRequest(
            String model,
            String manufacturer,
            Integer totalSeats,
            String baseAirport,
            Integer pilot1Id,
            Integer pilot2Id
    ) {}

    public record AircraftResponse(
            Integer aircraftID,
            String model,
            String manufacturer,
            Integer totalSeats,
            String baseAirport,
            String airline,      // enum name (e.g. AEGEAN) or displayName
            Integer pilot1Id,
            Integer pilot2Id
    ) {}

    // ===================== GET (list + optional filter) =====================
    // GET /api/aircraft
    // GET /api/aircraft?airline=KLM (works with enum name OR code OR display name)
    @GetMapping
    public List<AircraftResponse> all(@RequestParam(required = false) String airline) {
        List<Aircraft> list;

        if (airline == null || airline.isBlank()) {
            list = aircraftRepo.findAll();
        } else {
            Airline a = parseAirline(airline);
            list = aircraftRepo.findByAirline(a);
        }

        return list.stream().map(this::toRes).toList();
    }

    // ===================== GET one =====================
    @GetMapping("/{id}")
    public AircraftResponse one(@PathVariable Integer id) {
        Aircraft a = aircraftRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Aircraft not found: " + id));
        return toRes(a);
    }

    // ===================== POST =====================
    @PostMapping
    public AircraftResponse create(@RequestBody AircraftRequest req) {
        Aircraft a = new Aircraft();
        apply(a, req, true);
        return toRes(aircraftRepo.save(a));
    }

    // ===================== PUT =====================
    @PutMapping("/{id}")
    public AircraftResponse update(@PathVariable Integer id, @RequestBody AircraftRequest req) {
        Aircraft a = aircraftRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Aircraft not found: " + id));
        apply(a, req, false);
        return toRes(aircraftRepo.save(a));
    }

    // ===================== DELETE =====================
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        if (!aircraftRepo.existsById(id)) throw new NotFoundException("Aircraft not found: " + id);
        aircraftRepo.deleteById(id);
    }

    // ===================== Helpers =====================

    private Airline parseAirline(String airline) {
        String in = airline.trim();

        // 1) enum name e.g. "AEGEAN"
        try {
            return Airline.valueOf(in.toUpperCase());
        } catch (IllegalArgumentException ignored) {}

        // 2) code e.g. "AE"
        try {
            return Airline.fromCode(in);
        } catch (IllegalArgumentException ignored) {}

        // 3) displayName e.g. "Aegean"
        try {
            return Airline.fromDisplayName(in);
        } catch (IllegalArgumentException ignored) {}

        throw new ApiException("Invalid airline: " + airline);
    }

    private AircraftResponse toRes(Aircraft a) {
        return new AircraftResponse(
                a.getAircraftID(),
                a.getModel(),
                a.getManufacturer(),
                a.getTotalSeats(),
                a.getBaseAirport(),
                a.getAirline() == null ? null : a.getAirline().name(), // ή .getDisplayName()
                a.getPilot1() == null ? null : a.getPilot1().getPilotID(),
                a.getPilot2() == null ? null : a.getPilot2().getPilotID()
        );
    }

    /**
     * apply request fields to entity
     * @param isCreate if true, enforce required fields for create
     */
    private void apply(Aircraft a, AircraftRequest req, boolean isCreate) {

        if (req.model() != null) a.setModel(req.model());
        if (req.manufacturer() != null) a.setManufacturer(req.manufacturer());
        if (req.totalSeats() != null) a.setTotalSeats(req.totalSeats());

        // baseAirport required on create
        if (req.baseAirport() != null) {
            if (req.baseAirport().isBlank()) throw new ApiException("baseAirport cannot be blank");
            a.setBaseAirport(req.baseAirport().trim());
        } else if (isCreate && a.getBaseAirport() == null) {
            throw new ApiException("baseAirport is required");
        }

        // upcoming flights of this aircraft (only relevant for updates)
        LocalDateTime now = LocalDateTime.now();
        var upcoming = (a.getAircraftID() == null)
                ? List.<com.example.demo.model.Flights>of()
                : flightRepo.findByAircraft_AircraftIDAndDepartureTimeAfterOrderByDepartureTimeAsc(a.getAircraftID(), now);

        // pilot1
        if (req.pilot1Id() != null) {
            Pilot p1 = pilotRepo.findById(req.pilot1Id())
                    .orElseThrow(() -> new NotFoundException("Pilot not found: " + req.pilot1Id()));

            assertPilotCanBeAssigned(a, p1, upcoming);
            a.setPilot1(p1);

        } else if (!isCreate) {
            // unassign pilot1 (no availability check needed)
            a.setPilot1(null);
        }

        // pilot2
        if (req.pilot2Id() != null) {
            Pilot p2 = pilotRepo.findById(req.pilot2Id())
                    .orElseThrow(() -> new NotFoundException("Pilot not found: " + req.pilot2Id()));

            assertPilotCanBeAssigned(a, p2, upcoming);
            a.setPilot2(p2);

        } else if (!isCreate) {
            // unassign pilot2 (no availability check needed)
            a.setPilot2(null);
        }

        // cannot be same pilot
        if (a.getPilot1() != null && a.getPilot2() != null
                && a.getPilot1().getPilotID().equals(a.getPilot2().getPilotID())) {
            throw new ApiException("pilot1Id and pilot2Id cannot be the same");
        }
    }

    private void assertPilotCanBeAssigned(Aircraft aircraft, Pilot pilot, List<com.example.demo.model.Flights> upcoming) {
        // airline consistency
        if (pilot.getAirline() != null && aircraft.getAirline() != null && pilot.getAirline() != aircraft.getAirline()) {
            throw new ApiException("Pilot " + pilot.getPilotID() + " belongs to "
                    + pilot.getAirline().getDisplayName() + " and cannot be assigned to aircraft of "
                    + aircraft.getAirline().getDisplayName());
        }

        // If the aircraft already has scheduled future flights, the pilot must be free for all of them.
        for (var f : upcoming) {
            var depMinus3 = f.getDepartureTime().minusHours(3);
            var busyEnd = f.getArrivalTime().plusHours(3);

            long conflicts = flightRepo.countPilotConflicts(pilot.getPilotID(), depMinus3, busyEnd);
            if (conflicts > 0) {
                throw new ApiException("Pilot " + pilot.getPilotID()
                        + " is not available for upcoming flight " + f.getFlightNumber()
                        + " (3-hour rule). Choose another pilot or remove/reschedule the conflicting flight.");
            }
        }
    }
}