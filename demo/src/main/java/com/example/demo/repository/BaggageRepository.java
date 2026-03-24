package com.example.demo.repository;

import com.example.demo.model.Baggage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaggageRepository extends JpaRepository<Baggage, Integer> {
    List<Baggage> findByBooking_BookingID(Integer bookingId);
}
