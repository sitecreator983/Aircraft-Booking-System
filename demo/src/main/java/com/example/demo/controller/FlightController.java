package com.example.demo.controller;

import com.example.demo.model.Aircraft;
import com.example.demo.model.Airline;
import com.example.demo.model.Flights;
import com.example.demo.repository.AircraftRepository;
import com.example.demo.repository.FlightRepository;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Seats;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.SeatRepository;

import com.example.demo.model.Pilot;
import com.example.demo.repository.PilotRepository;

import java.util.List;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final SeatRepository seatRepo;
    private final BookingRepository bookingRepo;

    private final FlightRepository flightRepo;
    private final AircraftRepository aircraftRepo;
    private final PilotRepository pilotRepo;

    public FlightController(FlightRepository flightRepo, AircraftRepository aircraftRepo, SeatRepository seatRepo, BookingRepository bookingRepo, PilotRepository pilotRepo) {
        this.flightRepo = flightRepo;
        this.aircraftRepo = aircraftRepo;
        this.seatRepo = seatRepo;
        this.bookingRepo = bookingRepo;
        this.pilotRepo = pilotRepo;
    }

    record FlightRequest(
            String companyName,
            String flightDigits,
            String aircraftModel,   // από dropdown μοντέλων (για auto επιλογή)
            Integer aircraftId,     // optional (manual επιλογή)
            String departureAirport,
            String arrivalAirport,
            String departureTime,
            String arrivalTime
    ) {}

    record FlightResponse(
            String flightNumber,
            String companyName,
            Integer aircraftId,
            String departureAirport,
            String arrivalAirport,
            String departureTime,
            String arrivalTime
    ) {}

    private void assertAircraftAvailableAndAtDeparture(
            Aircraft aircraft,
            java.time.LocalDateTime dep,
            java.time.LocalDateTime arr,
            String departureAirport
    ) {
        // 3h rule: aircraft busy until arrival + 3 hours
        java.time.LocalDateTime newBusyEnd = arr.plusHours(3);
        java.time.LocalDateTime depMinus3 = dep.minusHours(3);

        long conflicts = flightRepo.countConflicts(
                aircraft.getAircraftID(),
                depMinus3,
                newBusyEnd
        );

        if (conflicts > 0) {
            throw new ApiException(
                    "Aircraft " + aircraft.getAircraftID()
                            + " is not available (already assigned within the 3-hour rule)."
            );
        }

        // Pilot availability + staffing
        assertPilotsAvailable(aircraft, dep, arr);

        // Location check: where is the aircraft at time dep?
        Flights last = flightRepo
                .findTopByAircraft_AircraftIDAndArrivalTimeLessThanEqualOrderByArrivalTimeDesc(
                        aircraft.getAircraftID(), dep
                );

        String expectedAirport = (last != null)
                ? last.getArrivalAirport()
                : aircraft.getBaseAirport();

        if (expectedAirport == null || expectedAirport.isBlank()) {
            throw new ApiException("Aircraft " + aircraft.getAircraftID()
                    + " has no baseAirport, cannot validate location.");
        }

        if (departureAirport == null || departureAirport.isBlank()) {
            throw new ApiException("departureAirport is required.");
        }

        if (!expectedAirport.equalsIgnoreCase(departureAirport.trim())) {
            throw new ApiException(
                    "Aircraft " + aircraft.getAircraftID()
                            + " is not at departure airport. Expected: " + expectedAirport
                            + ", got: " + departureAirport
            );
        }
    }


    private void assertPilotsAvailable(Aircraft aircraft, java.time.LocalDateTime dep, java.time.LocalDateTime arr) {
        if (aircraft.getPilot1() == null || aircraft.getPilot2() == null) {
            throw new ApiException("Aircraft " + aircraft.getAircraftID() + " must have 2 pilots assigned before scheduling a flight.");
        }

        // 3h rule for pilots as well: busy from dep-3h to arr+3h
        java.time.LocalDateTime depMinus3 = dep.minusHours(3);
        java.time.LocalDateTime busyEnd = arr.plusHours(3);

        Integer p1 = aircraft.getPilot1().getPilotID();
        Integer p2 = aircraft.getPilot2().getPilotID();

        long c1 = flightRepo.countPilotConflicts(p1, depMinus3, busyEnd);
        if (c1 > 0) {
            throw new ApiException("Pilot " + p1 + " is not available (already assigned to a flight within the 3-hour rule).");
        }

        long c2 = flightRepo.countPilotConflicts(p2, depMinus3, busyEnd);
        if (c2 > 0) {
            throw new ApiException("Pilot " + p2 + " is not available (already assigned to a flight within the 3-hour rule).");
        }
    }

    private void assertPilotsAvailableForExistingFlight(String flightNumber, Aircraft aircraft, java.time.LocalDateTime dep, java.time.LocalDateTime arr) {
        if (aircraft.getPilot1() == null || aircraft.getPilot2() == null) {
            throw new ApiException("Aircraft " + aircraft.getAircraftID() + " must have 2 pilots assigned.");
        }

        java.time.LocalDateTime depMinus3 = dep.minusHours(3);
        java.time.LocalDateTime busyEnd = arr.plusHours(3);

        Integer p1 = aircraft.getPilot1().getPilotID();
        Integer p2 = aircraft.getPilot2().getPilotID();

        long c1 = flightRepo.countPilotConflictsExcludingFlight(p1, depMinus3, busyEnd, flightNumber);
        if (c1 > 0) throw new ApiException("Pilot " + p1 + " is not available (already assigned to a flight within the 3-hour rule).");

        long c2 = flightRepo.countPilotConflictsExcludingFlight(p2, depMinus3, busyEnd, flightNumber);
        if (c2 > 0) throw new ApiException("Pilot " + p2 + " is not available (already assigned to a flight within the 3-hour rule).");
    }

    @GetMapping
    public List<FlightResponse> all() {
        return flightRepo.findAll().stream().map(this::toRes).toList();
    }

    @GetMapping("/{flightNumber}")
    public FlightResponse one(@PathVariable String flightNumber) {
        return toRes(flightRepo.findById(flightNumber)
                .orElseThrow(() -> new NotFoundException("Flight not found: " + flightNumber)));
    }

    public record FlightPilotsResponse(
        String flightNumber,
        Integer aircraftId,
        Integer pilot1Id,
        Integer pilot2Id
    ) {}

    public record UpdateFlightPilotsRequest(Integer pilot1Id, Integer pilot2Id) {}

    @GetMapping("/{flightNumber}/pilots")
    public FlightPilotsResponse getPilots(@PathVariable String flightNumber) {
        Flights f = flightRepo.findById(flightNumber)
            .orElseThrow(() -> new NotFoundException("Flight not found: " + flightNumber));

        Aircraft a = f.getAircraft();

        return new FlightPilotsResponse(
            f.getFlightNumber(),
            a.getAircraftID(),
            a.getPilot1() == null ? null : a.getPilot1().getPilotID(),
            a.getPilot2() == null ? null : a.getPilot2().getPilotID()
        );
    }

    @PutMapping("/{flightNumber}/pilots")
    public FlightPilotsResponse updatePilots(
        @PathVariable String flightNumber,
        @RequestBody UpdateFlightPilotsRequest req
    ) {
        Flights f = flightRepo.findById(flightNumber)
            .orElseThrow(() -> new NotFoundException("Flight not found: " + flightNumber));

        if (req == null || req.pilot1Id() == null || req.pilot2Id() == null) {
            throw new ApiException("pilot1Id and pilot2Id are required");
        }
        if (req.pilot1Id().equals(req.pilot2Id())) {
            throw new ApiException("pilot1Id and pilot2Id cannot be the same");
        }

        Aircraft aircraft = f.getAircraft();

        Pilot p1 = pilotRepo.findById(req.pilot1Id())
            .orElseThrow(() -> new NotFoundException("Pilot not found: " + req.pilot1Id()));
        Pilot p2 = pilotRepo.findById(req.pilot2Id())
            .orElseThrow(() -> new NotFoundException("Pilot not found: " + req.pilot2Id()));

        // Airline consistency
        if (p1.getAirline() != null && aircraft.getAirline() != null && p1.getAirline() != aircraft.getAirline()) {
            throw new ApiException("Pilot " + p1.getPilotID() + " belongs to "
                + p1.getAirline().getDisplayName() + " and cannot be assigned to aircraft of "
                + aircraft.getAirline().getDisplayName());
        }
        if (p2.getAirline() != null && aircraft.getAirline() != null && p2.getAirline() != aircraft.getAirline()) {
            throw new ApiException("Pilot " + p2.getPilotID() + " belongs to "
                + p2.getAirline().getDisplayName() + " and cannot be assigned to aircraft of "
                + aircraft.getAirline().getDisplayName());
        }

        aircraft.setPilot1(p1);
        aircraft.setPilot2(p2);

        // Same availability checks as create-flight, but exclude THIS flight
        assertPilotsAvailableForExistingFlight(f.getFlightNumber(), aircraft, f.getDepartureTime(), f.getArrivalTime());

        aircraftRepo.save(aircraft);

        return new FlightPilotsResponse(
            f.getFlightNumber(),
            aircraft.getAircraftID(),
            aircraft.getPilot1() == null ? null : aircraft.getPilot1().getPilotID(),
            aircraft.getPilot2() == null ? null : aircraft.getPilot2().getPilotID()
        );
    }

    @PostMapping
    public FlightResponse create(@RequestBody FlightRequest req) {

        // 1) Airline from companyName
        Airline airline;
        try {
            airline = Airline.fromDisplayName(req.companyName());
        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid companyName. Allowed values: " +
                    java.util.Arrays.toString(Airline.values()));
        }

        // 2) digits must be exactly 3 digits
        String digits = req.flightDigits();
        if (digits == null || !digits.matches("\\d{3}")) {
            throw new ApiException("flightDigits must be exactly 3 digits (e.g. \"123\").");
        }

        // 3) flightNumber = CODE + digits
        String flightNumber = airline.getCode() + digits;

        // 4) must be unique
        if (flightRepo.existsById(flightNumber)) {
            throw new ApiException("Flight number already exists: " + flightNumber);
        }

        // 5) parse times once
        java.time.LocalDateTime dep = java.time.LocalDateTime.parse(req.departureTime());
        java.time.LocalDateTime arr = java.time.LocalDateTime.parse(req.arrivalTime());

        if (!arr.isAfter(dep)) {
            throw new ApiException("arrivalTime must be after departureTime.");
        }

        if (req.departureAirport() == null || req.departureAirport().isBlank()) {
            throw new ApiException("departureAirport is required.");
        }
        if (req.arrivalAirport() == null || req.arrivalAirport().isBlank()) {
            throw new ApiException("arrivalAirport is required.");
        }

        Aircraft aircraft;

        // --- MANUAL επιλογή
        if (req.aircraftId() != null) {

            aircraft = aircraftRepo.findById(req.aircraftId())
                    .orElseThrow(() -> new NotFoundException("Aircraft not found with id: " + req.aircraftId()));

            // must belong to airline
            if (aircraft.getAirline() != airline) {
                throw new ApiException("Selected aircraft does not belong to " + airline.getDisplayName());
            }

            // if aircraftModel provided, must match
            if (req.aircraftModel() != null && !req.aircraftModel().isBlank()
                    && !req.aircraftModel().equals(aircraft.getModel())) {
                throw new ApiException("Selected aircraft model mismatch.");
            }

            // checks: availability + 3h rule + location at departure
            assertAircraftAvailableAndAtDeparture(aircraft, dep, arr, req.departureAirport());

        } else {
            // --- AUTO επιλογή βάσει airline + model

            if (req.aircraftModel() == null || req.aircraftModel().isBlank()) {
                throw new ApiException("aircraftModel is required when aircraftId is not provided.");
            }

            List<Aircraft> candidates = aircraftRepo.findByAirlineAndModel(airline, req.aircraftModel());

            if (candidates.isEmpty()) {
                throw new ApiException("No aircraft registered for " + airline.getDisplayName()
                        + " / " + req.aircraftModel());
            }

            Aircraft chosen = null;
            for (Aircraft a : candidates) {
                try {
                    assertAircraftAvailableAndAtDeparture(a, dep, arr, req.departureAirport());
                    chosen = a;
                    break;
                } catch (ApiException ignored) {
                    // this candidate doesn't work, try next
                }
            }

            if (chosen == null) {
                throw new ApiException(
                        "No aircraft available for " + airline.getDisplayName()
                                + " / " + req.aircraftModel()
                                + " at " + req.departureAirport()
                                + " for the selected time window (incl. 3h rule)."
                );
            }

            aircraft = chosen;
        }

        // 6) create entity + save
        Flights f = new Flights();
        f.setFlightNumber(flightNumber);
        f.setCompanyName(airline.getDisplayName());
        f.setAircraft(aircraft);
        f.setDepartureAirport(req.departureAirport().trim());
        f.setArrivalAirport(req.arrivalAirport().trim());
        f.setDepartureTime(dep);
        f.setArrivalTime(arr);


        // ✅ validate arrival airport code
        if (req.arrivalAirport() == null || req.arrivalAirport().isBlank()) {
            throw new ApiException("arrivalAirport is required");
        }
        if (!AirportController.isValidCode(req.arrivalAirport())) {
            throw new ApiException("Invalid arrivalAirport. Must be one of the allowed airports.");
        } 

        flightRepo.save(f);

        // 7) response
        return new FlightResponse(
                f.getFlightNumber(),
                f.getCompanyName(),
                f.getAircraft().getAircraftID(),
                f.getDepartureAirport(),
                f.getArrivalAirport(),
                f.getDepartureTime().toString(),
                f.getArrivalTime().toString()
        );
    }

    @DeleteMapping("/{flightNumber}")
    public void delete(@PathVariable String flightNumber) {
        if (!flightRepo.existsById(flightNumber)) throw new NotFoundException("Flight not found: " + flightNumber);
        flightRepo.deleteById(flightNumber);
    }

    private FlightResponse toRes(Flights f) {
        return new FlightResponse(
                f.getFlightNumber(),
                f.getCompanyName(),
                f.getAircraft().getAircraftID(),
                f.getDepartureAirport(),
                f.getArrivalAirport(),
                f.getDepartureTime().toString(),
                f.getArrivalTime().toString()
        );
    }

    record SeatResponse(Integer seatID, Integer aircraftID, String seatNumber, String seatClass) {}

    @GetMapping("/{flightNumber}/available-seats")
    public List<SeatResponse> availableSeats(
        @PathVariable String flightNumber,
        @RequestParam(required = false) String seatClass
    ) {
        Flights flight = flightRepo.findById(flightNumber)
            .orElseThrow(() -> new NotFoundException("Flight not found: " + flightNumber));

        Integer aircraftId = flight.getAircraft().getAircraftID();

        List<Seats> allSeats = (seatClass == null || seatClass.isBlank())
            ? seatRepo.findByAircraft_AircraftID(aircraftId)
            : seatRepo.findByAircraft_AircraftIDAndSeatClassIgnoreCase(aircraftId, seatClass);

        List<Integer> booked = bookingRepo.findBookedSeatIdsByFlightNumber(flightNumber);

        return allSeats.stream()
            .filter(s -> !booked.contains(s.getSeatID()))
            .map(s -> new SeatResponse(
                s.getSeatID(),
                s.getAircraft().getAircraftID(),
                s.getSeatNumber(),
                s.getSeatClass()
            ))
            .toList();
    }
}