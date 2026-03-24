package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Seats;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seats, Integer> {

    long countByAircraft_AircraftID(Integer aircraftID);
    List<Seats> findByAircraft_AircraftID(Integer aircraftId);
    Optional<Seats> findByAircraft_AircraftIDAndSeatNumber(Integer aircraftId, String seatNumber);
    List<Seats> findByAircraft_AircraftIDAndSeatClassIgnoreCase(Integer aircraftId, String seatClass);
}
