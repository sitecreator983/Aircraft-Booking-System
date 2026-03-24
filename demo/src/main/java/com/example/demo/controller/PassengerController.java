package com.example.demo.controller;

import com.example.demo.model.Passenger;
import com.example.demo.repository.PassengerRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/passengers")
public class PassengerController {

    private final PassengerRepository repo;

    public PassengerController(PassengerRepository repo) {
        this.repo = repo;
    }

    public record PassengerRequest(String firstName, String lastName, String passportNumber, String email) {}
    public record PassengerResponse(Integer passengerID, String firstName, String lastName, String passportNumber, String email) {}

    @GetMapping
    public List<PassengerResponse> all() {
        return repo.findAll().stream().map(this::toRes).toList();
    }

    @GetMapping("/{id}")
    public PassengerResponse one(@PathVariable Integer id) {
        return toRes(repo.findById(id).orElseThrow(() -> new NotFoundException("Passenger not found: " + id)));
    }

    @PostMapping
    public PassengerResponse create(@RequestBody PassengerRequest req) {
        Passenger p = new Passenger();
        if (req.firstName() != null) p.setFirstName(req.firstName());
        if (req.lastName() != null) p.setLastName(req.lastName());
        if (req.passportNumber() != null) p.setPassportNumber(req.passportNumber());
        if (req.email() != null) p.setEmail(req.email());
        return toRes(repo.save(p));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        if (!repo.existsById(id)) throw new NotFoundException("Passenger not found: " + id);
        repo.deleteById(id);
    }

    private PassengerResponse toRes(Passenger p) {
        return new PassengerResponse(p.getPassengerID(), p.getFirstName(), p.getLastName(), p.getPassportNumber(), p.getEmail());
    }
}