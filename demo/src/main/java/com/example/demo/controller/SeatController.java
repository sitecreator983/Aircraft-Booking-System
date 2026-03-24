package com.example.demo.controller;

import com.example.demo.model.Aircraft;
import com.example.demo.model.Seats;
import com.example.demo.repository.AircraftRepository;
import com.example.demo.repository.SeatRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SeatController {

    private final SeatRepository seatRepo;
    private final AircraftRepository aircraftRepo;

    public SeatController(SeatRepository seatRepo, AircraftRepository aircraftRepo) {
        this.seatRepo = seatRepo;
        this.aircraftRepo = aircraftRepo;
    }

    public record SeatRequest(String seatNumber, String seatClass) {}
    public record SeatResponse(Integer seatID, Integer aircraftID, String seatNumber, String seatClass) {}

    @GetMapping("/api/aircraft/{aircraftId}/seats")
    public List<SeatResponse> list(@PathVariable Integer aircraftId) {
        return seatRepo.findByAircraft_AircraftID(aircraftId).stream().map(this::toRes).toList();
    }

    @PostMapping("/api/aircraft/{aircraftId}/seats")
    public SeatResponse create(@PathVariable Integer aircraftId, @RequestBody SeatRequest req) {
        if (req.seatNumber() == null || req.seatNumber().isBlank()) throw new ApiException("seatNumber is required");
        if (req.seatClass() == null || req.seatClass().isBlank()) throw new ApiException("seatClass is required");

        Aircraft a = aircraftRepo.findById(aircraftId)
                .orElseThrow(() -> new NotFoundException("Aircraft not found: " + aircraftId));

        seatRepo.findByAircraft_AircraftIDAndSeatNumber(aircraftId, req.seatNumber())
                .ifPresent(x -> { throw new ApiException("Seat already exists for this aircraft: " + req.seatNumber()); });

        Seats s = new Seats();
        s.setAircraft(a);
        s.setSeatNumber(req.seatNumber());
        s.setSeatClass(req.seatClass());

        return toRes(seatRepo.save(s));
    }

    @GetMapping("/api/seats/{seatId}")
    public SeatResponse one(@PathVariable Integer seatId) {
        Seats s = seatRepo.findById(seatId).orElseThrow(() -> new NotFoundException("Seat not found: " + seatId));
        return toRes(s);
    }

    @DeleteMapping("/api/seats/{seatId}")
    public void delete(@PathVariable Integer seatId) {
        if (!seatRepo.existsById(seatId)) throw new NotFoundException("Seat not found: " + seatId);
        seatRepo.deleteById(seatId);
    }

    private SeatResponse toRes(Seats s) {
        return new SeatResponse(
                s.getSeatID(),
                s.getAircraft().getAircraftID(),
                s.getSeatNumber(),
                s.getSeatClass()
        );
    }
}