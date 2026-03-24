package com.example.demo.repository;

import com.example.demo.model.Aircraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import com.example.demo.model.Airline;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface AircraftRepository extends JpaRepository<Aircraft, Integer> {
    List<Aircraft> findByManufacturer(String manufacturer);
    List<Aircraft> findByModel(String model);
    List<Aircraft> findByAirlineAndModel(Airline airline, String model);
    List<Aircraft> findByAirline(Airline airline);

    @Query("""
    select a from Aircraft a
    where a.airline = :airline
      and a.model = :model
      and a.aircraftID not in (
        select f.aircraft.aircraftID from Flights f
        where f.departureTime < :arrival
          and f.arrivalTime > :departure
      )
    order by a.aircraftID asc
    """)
    Optional<Aircraft> findFirstAvailable(
        @Param("airline") Airline airline,
        @Param("model") String model,
        @Param("departure") LocalDateTime departure,
        @Param("arrival") LocalDateTime arrival
    );
}
