package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Bookings;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingRepository extends JpaRepository<Bookings, Integer> {
    List<Bookings> findByPassenger_PassengerID(Integer passengerId);
    List<Bookings> findByFlight_FlightNumber(String flightNumber);
    List<Bookings> findBySeat_SeatID(Integer seatId);

    boolean existsByFlight_FlightNumberAndSeat_SeatID(String flightNumber, Integer seatId);

    @Query("select b.seat.seatID from Bookings b where b.flight.flightNumber = :flightNumber")
    List<Integer> findBookedSeatIdsByFlightNumber(@Param("flightNumber") String flightNumber);
}
