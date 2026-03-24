package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.Flights;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;


public interface FlightRepository extends JpaRepository<Flights, String> {
    List<Flights> findByAircraft_AircraftID(Integer aircraftId);
    List<Flights> findByAircraft_AircraftIDAndDepartureTimeAfterOrderByDepartureTimeAsc(Integer aircraftId, LocalDateTime time);
    List<Flights> findByDepartureAirport(String departureAirport);
    List<Flights> findByArrivalAirport(String arrivalAirport);

    @Query("""
        select count(f) from Flights f
        where f.aircraft.aircraftID = :aircraftId
            and f.departureTime < :newBusyEnd
            and f.arrivalTime > :depMinus3
    """)
    long countConflicts(
        @Param("aircraftId") Integer aircraftId,
        @Param("depMinus3") LocalDateTime depMinus3,
        @Param("newBusyEnd") LocalDateTime newBusyEnd
    );

    @Query("""
        select count(f) from Flights f
        join f.aircraft a
        where (a.pilot1.pilotID = :pilotId or a.pilot2.pilotID = :pilotId)
          and f.departureTime < :newBusyEnd
          and f.arrivalTime > :depMinus3
    """)
    long countPilotConflicts(
        @Param("pilotId") Integer pilotId,
        @Param("depMinus3") LocalDateTime depMinus3,
        @Param("newBusyEnd") LocalDateTime newBusyEnd
    );

    @Query("""
        select count(f) from Flights f
        join f.aircraft a
        where (a.pilot1.pilotID = :pilotId or a.pilot2.pilotID = :pilotId)
          and f.flightNumber <> :excludeFlightNumber
          and f.departureTime < :newBusyEnd
          and f.arrivalTime > :depMinus3
    """)
    long countPilotConflictsExcludingFlight(
        @Param("pilotId") Integer pilotId,
        @Param("depMinus3") LocalDateTime depMinus3,
        @Param("newBusyEnd") LocalDateTime newBusyEnd,
        @Param("excludeFlightNumber") String excludeFlightNumber
    );

    Flights findTopByAircraft_AircraftIDAndArrivalTimeLessThanEqualOrderByArrivalTimeDesc(
        Integer aircraftId,
        LocalDateTime time
    );
}
