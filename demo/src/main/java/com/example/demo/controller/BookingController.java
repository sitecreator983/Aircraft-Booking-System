package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingRepository bookingRepo;
    private final PassengerRepository passengerRepo;
    private final FlightRepository flightRepo;
    private final SeatRepository seatRepo;

    public BookingController(
            BookingRepository bookingRepo,
            PassengerRepository passengerRepo,
            FlightRepository flightRepo,
            SeatRepository seatRepo
    ) {
        this.bookingRepo = bookingRepo;
        this.passengerRepo = passengerRepo;
        this.flightRepo = flightRepo;
        this.seatRepo = seatRepo;
    }

    public record BookingCreateRequest(Integer passengerId, String flightNumber, Integer seatId, String bookingDate) {}
    public record BookingResponse(
        Integer bookingId,
        String passengerName,
        String passengerEmail,
        String flightNumber,
        Integer seatId,
        String seatNumber,
        String createdAt
    ) {}

    @GetMapping
    public List<BookingResponse> all(
            @RequestParam(required = false) Integer passengerId,
            @RequestParam(required = false) String flightNumber
    ) {
        List<Bookings> list;
        if (passengerId != null) list = bookingRepo.findByPassenger_PassengerID(passengerId);
        else if (flightNumber != null && !flightNumber.isBlank()) list = bookingRepo.findByFlight_FlightNumber(flightNumber);
        else list = bookingRepo.findAll();

        return list.stream().map(this::toRes).toList();
    }

    @PostMapping
    public BookingResponse create(@RequestBody BookingCreateRequest req) {
        if (req.passengerId() == null) throw new ApiException("passengerId is required");
        if (req.flightNumber() == null || req.flightNumber().isBlank()) throw new ApiException("flightNumber is required");
        if (req.seatId() == null) throw new ApiException("seatId is required");

        Passenger passenger = passengerRepo.findById(req.passengerId())
                .orElseThrow(() -> new NotFoundException("Passenger not found: " + req.passengerId()));

        Flights flight = flightRepo.findById(req.flightNumber())
                .orElseThrow(() -> new NotFoundException("Flight not found: " + req.flightNumber()));

        Seats seat = seatRepo.findById(req.seatId())
                .orElseThrow(() -> new NotFoundException("Seat not found: " + req.seatId()));

        // seat must belong to flight aircraft
        if (!flight.getAircraft().getAircraftID().equals(seat.getAircraft().getAircraftID())) {
            throw new ApiException("Seat does not belong to the aircraft of this flight");
        }

        // seat cannot be double-booked for same flight
        if (bookingRepo.existsByFlight_FlightNumberAndSeat_SeatID(req.flightNumber(), req.seatId())) {
            throw new ApiException("Seat already booked for this flight");
        }

        Bookings b = new Bookings();
        b.setPassenger(passenger);
        b.setFlight(flight);
        b.setSeat(seat);

        b.setBookingDate(
                (req.bookingDate() == null || req.bookingDate().isBlank())
                        ? LocalDateTime.now()
                        : LocalDateTime.parse(req.bookingDate())
        );

        return toRes(bookingRepo.save(b));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        if (!bookingRepo.existsById(id)) throw new NotFoundException("Booking not found: " + id);
        bookingRepo.deleteById(id);
    }

    private BookingResponse toRes(Bookings b) {
        String first = b.getPassenger() != null ? b.getPassenger().getFirstName() : null;
        String last  = b.getPassenger() != null ? b.getPassenger().getLastName() : null;

        String fullName = null;
        if (first != null || last != null) {
            fullName = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
            if (fullName.isBlank()) fullName = null;
        }

        return new BookingResponse(
            b.getBookingID(), // στέλνουμε ως bookingId στο JSON
            fullName,
            b.getPassenger() == null ? null : b.getPassenger().getEmail(),
            b.getFlight() == null ? null : b.getFlight().getFlightNumber(),
            b.getSeat() == null ? null : b.getSeat().getSeatID(),
            b.getSeat() == null ? null : b.getSeat().getSeatNumber(),
            b.getBookingDate() == null ? null : b.getBookingDate().toString() // createdAt
        );
    }
}