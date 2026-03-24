package com.example.demo.repository;

import com.example.demo.model.Pilot;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.Airline;

import java.util.List;
import java.util.Optional;

public interface PilotRepository extends JpaRepository<Pilot, Integer> {
    Optional<Pilot> findByLicenseNumber(String licenseNumber);
    Optional<Pilot> findByEmail(String email);
    List<Pilot> findByAirline(Airline airline);
}
