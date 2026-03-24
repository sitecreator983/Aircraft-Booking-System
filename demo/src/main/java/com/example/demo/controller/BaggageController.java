package com.example.demo.controller;

import com.example.demo.model.Baggage;
import com.example.demo.model.Bookings;
import com.example.demo.repository.BaggageRepository;
import com.example.demo.repository.BookingRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
public class BaggageController {

    private final BaggageRepository baggageRepo;
    private final BookingRepository bookingRepo;

    public BaggageController(BaggageRepository baggageRepo, BookingRepository bookingRepo) {
        this.baggageRepo = baggageRepo;
        this.bookingRepo = bookingRepo;
    }

    public record BaggageRequest(BigDecimal weightKg, String baggageType) {}
    public record BaggageResponse(Integer baggageID, Integer bookingID, BigDecimal weightKg, String baggageType) {}

    @GetMapping("/api/bookings/{bookingId}/baggage")
    public List<BaggageResponse> list(@PathVariable Integer bookingId) {
        return baggageRepo.findByBooking_BookingID(bookingId).stream().map(this::toRes).toList();
    }

    @PostMapping("/api/bookings/{bookingId}/baggage")
    public BaggageResponse create(@PathVariable Integer bookingId, @RequestBody BaggageRequest req) {
        Bookings booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));

        if (req.weightKg() != null && req.weightKg().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException("weightKg must be >= 0");
        }

        Baggage b = new Baggage();
        b.setBooking(booking);
        b.setWeightKg(req.weightKg());
        b.setBaggageType(req.baggageType());

        return toRes(baggageRepo.save(b));
    }

    @DeleteMapping("/api/baggage/{baggageId}")
    public void delete(@PathVariable Integer baggageId) {
        if (!baggageRepo.existsById(baggageId)) throw new NotFoundException("Baggage not found: " + baggageId);
        baggageRepo.deleteById(baggageId);
    }

    private BaggageResponse toRes(Baggage b) {
        return new BaggageResponse(
            b.getBaggageID(),
            b.getBooking().getBookingID(),
            b.getWeightKg(),
            b.getBaggageType()
        );
    }

    @ExceptionHandler(com.example.demo.auth.AuthExceptions.Unauthorized.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleUnauthorized(RuntimeException ex) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", 401,
                "error", "Unauthorized",
                "message", ex.getMessage()
        );
    }

    @ExceptionHandler(com.example.demo.auth.AuthExceptions.Locked.class)
    @ResponseStatus(HttpStatus.LOCKED) // 423
    public Map<String, Object> handleLocked(RuntimeException ex) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", 423,
                "error", "Locked",
                "message", ex.getMessage()
        );
    }


}